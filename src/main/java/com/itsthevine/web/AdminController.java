package com.itsthevine.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.itsthevine.web.domain.ContactEnquiry;
import com.itsthevine.web.domain.ContactEnquiryRepository;
import com.itsthevine.web.domain.Product;
import com.itsthevine.web.domain.ProductRepository;

import net.thebennett.platform.storage.StorageService;

/**
 * Everything behind the login: the catalogue, and the enquiries people have sent.
 *
 * <p>The whole of {@code /api/admin/**} is gated by {@code platform.security.authenticated-paths}, so
 * any signed-in Authentik user is an administrator here. That is deliberate for a two-person bakery —
 * the alternative is a role model nobody would maintain.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ProductRepository products;
    private final ContactEnquiryRepository enquiries;
    private final StorageService storage;
    private final String bucket;
    private final String publicBaseUrl;

    public AdminController(ProductRepository products, ContactEnquiryRepository enquiries,
                           StorageService storage,
                           @Value("${vine.storage.bucket:itsthevine}") String bucket,
                           @Value("${site.assets.base-url:https://s3.thebennett.net/itsthevine}") String publicBaseUrl) {
        this.products = products;
        this.enquiries = enquiries;
        this.storage = storage;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    // ---- products ----

    /** @param imageKeys bucket keys, in display order; the first is the one the card shows */
    public record ProductForm(String name, String category, Integer position, List<String> imageKeys) {}

    public record AdminProduct(Long id, String name, String category, int position,
                               List<String> imageKeys, List<String> imageUrls) {}

    @GetMapping("/products")
    @Transactional(readOnly = true)
    public List<AdminProduct> list() {
        return products.findAllByOrderByPositionAsc().stream().map(this::toAdmin).toList();
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public AdminProduct create(@RequestBody ProductForm form) {
        validate(form);
        // Default to the end of the list so a new product does not silently displace an existing one.
        int position = form.position() != null ? form.position() : nextPosition();
        return toAdmin(products.save(new Product(form.name().trim(), form.category().trim(),
                position, cleanKeys(form.imageKeys()))));
    }

    @PutMapping("/products/{id}")
    @Transactional
    public AdminProduct update(@PathVariable Long id, @RequestBody ProductForm form) {
        validate(form);
        Product p = products.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "no such product"));
        p.update(form.name().trim(), form.category().trim(),
                form.position() != null ? form.position() : p.getPosition(),
                cleanKeys(form.imageKeys()));
        return toAdmin(p);
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable Long id) {
        if (!products.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "no such product");
        }
        // The photos stay in the bucket: they are cheap, and an accidental delete is recoverable if
        // the images survive it.
        products.deleteById(id);
    }

    // ---- enquiries ----

    /** @param delivered false means the relay refused it and nobody was notified */
    public record AdminEnquiry(Long id, String name, String email, String message,
                               boolean delivered, Instant receivedAt) {}

    @GetMapping("/enquiries")
    @Transactional(readOnly = true)
    public List<AdminEnquiry> enquiries() {
        return enquiries.findAllByOrderByCreatedAtDesc().stream()
                .map(e -> new AdminEnquiry(e.getId(), e.getName(), e.getEmail(), e.getMessage(),
                        e.isDelivered(), e.getCreatedAt()))
                .toList();
    }

    // ---- photo upload ----

    /**
     * @param key       what to store on the product
     * @param uploadUrl short-lived; the browser PUTs the file straight to the bucket so the photo
     *                  never passes through this app
     * @param publicUrl where it will be readable from afterwards
     */
    public record UploadTarget(String key, String uploadUrl, String publicUrl) {}

    @PostMapping("/images/presign-upload")
    public UploadTarget presignUpload(@RequestParam String filename,
                                      @RequestParam(defaultValue = "application/octet-stream") String contentType) {
        // A UUID prefix rather than the bare filename: two people uploading "cake.jpg" must not
        // overwrite each other, and the bucket is public so keys should not be guessable.
        String safe = filename.toLowerCase().replaceAll("[^a-z0-9._-]", "-");
        String key = "images/products/" + UUID.randomUUID() + "-" + safe;
        return new UploadTarget(key.substring("images/".length()),
                storage.presignPut(bucket, key, contentType).toString(),
                publicBaseUrl + "/" + key);
    }

    // ---- helpers ----

    private void validate(ProductForm form) {
        if (form.name() == null || form.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "a product needs a name");
        }
        if (form.category() == null || form.category().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "a product needs a category");
        }
        if (form.imageKeys() == null || cleanKeys(form.imageKeys()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "a product needs at least one photo");
        }
    }

    private static List<String> cleanKeys(List<String> keys) {
        return keys == null ? List.of()
                : keys.stream().filter(k -> k != null && !k.isBlank()).map(String::trim).toList();
    }

    private int nextPosition() {
        return products.findAllByOrderByPositionAsc().stream()
                .mapToInt(Product::getPosition).max().orElse(0) + 1;
    }

    private AdminProduct toAdmin(Product p) {
        return new AdminProduct(p.getId(), p.getName(), p.getCategory(), p.getPosition(),
                p.getImageKeys(), p.getImageKeys().stream().map(this::publicUrl).toList());
    }

    private String publicUrl(String key) {
        return publicBaseUrl + "/images/" + key.replaceAll("^/+", "");
    }
}

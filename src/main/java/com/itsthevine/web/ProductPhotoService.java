package com.itsthevine.web;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import net.thebennett.platform.storage.StorageService;

/**
 * Turns whatever came off a phone into the one shape the bucket holds: a resized webp.
 *
 * The photos already in the bucket were re-encoded by hand once (50 MB of originals became 14 MB).
 * Uploads go through the same treatment so the catalogue doesn't slowly fill with 12 MP JPEGs, and
 * so nothing arrives carrying the GPS coordinates of the bakery's kitchen — decoding to a
 * {@link BufferedImage} and re-encoding drops every EXIF tag, because a raster has nowhere to put
 * them.
 *
 * Encoding shells out to {@code cwebp}. No pure-Java webp *writer* exists (TwelveMonkeys and
 * NightMonkeys both read only), and the libraries that do write bundle glibc natives that will not
 * load on the Alpine runtime — so the Dockerfile installs Alpine's own musl build of libwebp-tools
 * and we hand it bytes.
 */
@Service
public class ProductPhotoService {

    private static final Logger log = LoggerFactory.getLogger(ProductPhotoService.class);

    /** Big enough for a full-bleed card on a retina screen; far smaller than anything a phone shoots. */
    private static final int MAX_EDGE = 2000;
    private static final int QUALITY = 82;
    private static final long ENCODE_TIMEOUT_SECONDS = 30;

    private final StorageService storage;
    private final String bucket;
    private final String keyPrefix;

    public ProductPhotoService(StorageService storage,
                               @Value("${site.assets.bucket:itsthevine}") String bucket,
                               @Value("${site.assets.key-prefix:images/}") String keyPrefix) {
        this.storage = storage;
        this.bucket = bucket;
        // ProductCatalog builds public URLs as <base>/images/<stored key>, so the object itself lives
        // one level deeper than the key we persist. Keeping the prefix here means the database keeps
        // storing exactly what it stores today.
        this.keyPrefix = keyPrefix.replaceAll("^/+", "");
    }

    /**
     * @return the key to persist on the product — bucket-relative and WITHOUT the {@code images/}
     *         prefix, matching everything already in {@code product_image}
     */
    public String store(byte[] original, String filename, String nameHint) {
        BufferedImage decoded = decode(original, filename);
        byte[] webp = encodeWebp(resize(decoded));

        String key = "products/" + slug(nameHint) + "-" + token() + ".webp";
        storage.put(bucket, keyPrefix + key, webp, "image/webp");
        log.info("stored product photo {} ({} KB from {} KB)", key, webp.length / 1024, original.length / 1024);
        return key;
    }

    private BufferedImage decode(byte[] bytes, String filename) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                // ImageIO returns null rather than throwing when no reader claims the bytes — HEIC off
                // an iPhone lands here, as does anything that isn't really an image.
                throw new IllegalArgumentException(
                        "That file isn't an image we can read (" + filename + "). JPEG or PNG works.");
            }
            return image;
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read " + filename + ".", e);
        }
    }

    private BufferedImage resize(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        double scale = Math.min(1.0, (double) MAX_EDGE / Math.max(width, height));
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        // TYPE_INT_RGB regardless of scale: it flattens any alpha channel onto a known background and
        // gives cwebp a predictable input. The photos are opaque product shots.
        BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    /**
     * Temp files rather than piping through stdin/stdout: cwebp's stream handling varies by build, and
     * a couple of files in the container's tmpdir is a cheaper bet than debugging that in production.
     */
    private byte[] encodeWebp(BufferedImage image) {
        Path png = null;
        Path webp = null;
        try {
            png = Files.createTempFile("vine-photo-", ".png");
            webp = Files.createTempFile("vine-photo-", ".webp");
            if (!ImageIO.write(image, "png", png.toFile())) {
                throw new IllegalStateException("No PNG writer available to hand cwebp.");
            }

            Process process = new ProcessBuilder(
                    "cwebp", "-quiet", "-q", String.valueOf(QUALITY),
                    png.toString(), "-o", webp.toString())
                    .redirectErrorStream(true)
                    .start();

            if (!process.waitFor(ENCODE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("Encoding that photo took too long.");
            }
            if (process.exitValue() != 0) {
                String output = new String(process.getInputStream().readAllBytes()).trim();
                throw new IllegalStateException("Could not convert that photo. " + output);
            }
            return Files.readAllBytes(webp);
        } catch (IOException e) {
            // The usual cause is cwebp not being installed — worth saying so plainly.
            throw new IllegalStateException("Photo conversion is unavailable on this server.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Photo conversion was interrupted.", e);
        } finally {
            delete(png);
            delete(webp);
        }
    }

    private void delete(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("could not clean up {}", path, e);
        }
    }

    private String slug(String value) {
        String slug = value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return slug.isBlank() ? "photo" : slug;
    }

    /** Short random suffix so re-uploading the same dish never overwrites the previous photo. */
    private String token() {
        byte[] bytes = new byte[4];
        ThreadLocalRandom.current().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}

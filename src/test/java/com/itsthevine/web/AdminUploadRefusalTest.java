package com.itsthevine.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * A photo too big for the limit comes back as a sentence, not a stack trace.
 *
 * <p>Photo uploads used to fail at 1 MB — no multipart limits were configured, so Boot's default applied
 * (see {@code AdminPagesTest#photosOffAPhoneFitInsideTheUploadLimits}, which pins the numbers). Raising
 * them fixes the everyday case; this covers what the editor sees when a file really is too large, because
 * what happened before was a 500 followed by a second failure forwarding to {@code /error} — that forward
 * re-parsed the same over-sized request and threw again.
 *
 * <p>Standalone rather than a booted context, and the throw is staged from the service rather than from a
 * genuinely huge upload, because MockMvc does not enforce the container's multipart limits — there is no
 * way to provoke the real parse failure here. What that leaves worth asserting is the wiring: that the
 * handler catches this exception type, writes the same {@code problem} flash the other refusals use, and
 * redirects instead of rendering an error page. Whether the exception can reach a handler at all is a
 * property of {@code resolve-lazily}, which is asserted separately.
 */
class AdminUploadRefusalTest {

    @Test
    void anOversizePhotoIsRefusedOnThePageRatherThanAsAStackTrace() throws Exception {
        Catalogue catalogue = mock(Catalogue.class);
        doThrow(new MaxUploadSizeExceededException(15_728_640L))
                .when(catalogue).addPhotos(eq(7L), any());

        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AdminController(catalogue)).build();

        mvc.perform(multipart("/admin/items/7/photos")
                        .file(new MockMultipartFile("photos", "cake.jpg", "image/jpeg", new byte[] {1, 2, 3})))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                // The same flash key the domain's own refusals use, so it lands in the same place on the
                // page, and it names the limit rather than saying "invalid".
                .andExpect(flash().attribute("problem", containsString("too large")))
                .andExpect(flash().attribute("problem", containsString("15 MB")))
                // It should not tell the baker to go and shrink the photo: resizing is this app's job.
                .andExpect(flash().attribute("problem", containsString("resize it here")));
    }
}

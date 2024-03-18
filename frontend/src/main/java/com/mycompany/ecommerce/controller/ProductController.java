package com.mycompany.ecommerce.controller;

import com.mycompany.ecommerce.model.Product;
import com.mycompany.ecommerce.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    final Logger logger = LoggerFactory.getLogger(getClass());

    ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;

    }

    @GetMapping(value = {"", "/"})
    public @Nonnull
    Iterable<Product> getProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public @Nonnull Product getProduct(@PathVariable("id") long id) {
        return productService.getProduct(id);
    }

    @GetMapping(path = "resizedImg/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<ByteArrayResource> getImage(@PathVariable("id") long id) throws Exception {
        long nanosBefore = System.nanoTime();
        try {
            Callable<ResponseEntity<ByteArrayResource>> callable = () -> {
                try (InputStream originalImageAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("img/sweet-celebration-decoration-food-christmas-dessert-768342-pxhere.com.jpg")) {
                    BufferedImage resizedImage = resizeImage(ImageIO.read(Objects.requireNonNull(originalImageAsStream)), 50, 50);
                    MediaType contentType = MediaType.IMAGE_JPEG;
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    ImageIO.write(resizedImage, "jpg", out);
                    return ResponseEntity.ok()
                            .contentType(contentType)
                            .body(new ByteArrayResource(out.toByteArray()));
                }
            };

            // FIXME java.lang.UnsatisfiedLinkError: no asyncProfiler in java.library.path:
            // return Pyroscope.LabelsWrapper.run(new LabelsSet("spanId", Span.current().getSpanContext().getSpanId()), callable);
            return callable.call();
        } finally {
            logger.info("Generated image {} in {} ms", id, TimeUnit.MILLISECONDS.convert(System.nanoTime() - nanosBefore, TimeUnit.NANOSECONDS));
        }
    }

    /**
     * https://www.baeldung.com/java-resize-image
     */
    BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight)  {
        Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_DEFAULT);
        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        outputImage.getGraphics().drawImage(resultingImage, 0, 0, null);
        return outputImage;
    }

}

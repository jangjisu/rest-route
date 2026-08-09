package com.restroute.service.image.util;

import com.restroute.service.image.dto.RestStopImageData;
import com.restroute.service.image.exception.InvalidRestStopImageException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class RestStopImageProcessor {

    private static final int DETAIL_MAX_LONG_SIDE = 1600;
    private static final int LIST_MAX_LONG_SIDE = 480;
    private static final long MAX_PIXEL_COUNT = 30_000_000;
    static final float DETAIL_QUALITY = 0.80f;
    static final float LIST_QUALITY = 0.75f;
    private static final Set<String> SUPPORTED_FORMATS = Set.of("jpeg", "png");

    public RestStopImageData process(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidRestStopImageException("Image file is required");
        }

        try (InputStream input = file.getInputStream();
                ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            return processImage(imageInput);
        } catch (IOException exception) {
            throw new InvalidRestStopImageException("Unable to read image file", exception);
        }
    }

    private RestStopImageData processImage(ImageInputStream imageInput) throws IOException {
        Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
        if (!readers.hasNext()) {
            throw new InvalidRestStopImageException("Unsupported image format");
        }

        ImageReader reader = readers.next();
        try {
            reader.setInput(imageInput, true, true);
            validateFormat(reader.getFormatName());
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            validatePixelCount(width, height);
            BufferedImage source = reader.read(0);

            return new RestStopImageData(
                    encode(resize(source, DETAIL_MAX_LONG_SIDE), DETAIL_QUALITY),
                    encode(resize(source, LIST_MAX_LONG_SIDE), LIST_QUALITY));
        } finally {
            reader.dispose();
        }
    }

    private void validateFormat(String formatName) {
        if (!SUPPORTED_FORMATS.contains(formatName.toLowerCase(Locale.ROOT))) {
            throw new InvalidRestStopImageException("Only JPEG and PNG images are supported");
        }
    }

    private void validatePixelCount(int width, int height) {
        if ((long) width * height > MAX_PIXEL_COUNT) {
            throw new InvalidRestStopImageException("Image is too large");
        }
    }

    private BufferedImage resize(BufferedImage source, int maxLongSide) {
        int sourceLongSide = Math.max(source.getWidth(), source.getHeight());
        double scale = Math.min(1.0, (double) maxLongSide / sourceLongSide);
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        int type = source.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage resized = new BufferedImage(width, height, type);
        Graphics2D graphics = resized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return resized;
    }

    private byte[] encode(BufferedImage image, float quality) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/webp").next();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
                ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionType("Lossy");
            writeParam.setCompressionQuality(quality);
            writer.write(null, new IIOImage(image, null, null), writeParam);
            return output.toByteArray();
        } finally {
            writer.dispose();
        }
    }
}

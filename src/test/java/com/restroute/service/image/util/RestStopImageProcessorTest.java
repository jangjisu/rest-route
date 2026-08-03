package com.restroute.service.image.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.restroute.service.image.dto.RestStopImageData;
import com.restroute.service.image.exception.InvalidRestStopImageException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class RestStopImageProcessorTest {

    private final RestStopImageProcessor processor = new RestStopImageProcessor();

    @Test
    void createsLandscapeDetailAndListWebpVariants() throws IOException {
        RestStopImageData result = processor.process(image("jpeg", 2400, 1200));

        assertDimensions(result.detailImageData(), 1600, 800);
        assertDimensions(result.listImageData(), 480, 240);
    }

    @Test
    void usesWebpForBothVariantsAtTheConfiguredQualities() throws IOException {
        RestStopImageData result = processor.process(image("jpeg", 2400, 1200));

        assertWebp(result.detailImageData());
        assertWebp(result.listImageData());
        assertThat(RestStopImageProcessor.DETAIL_QUALITY).isEqualTo(0.80f);
        assertThat(RestStopImageProcessor.LIST_QUALITY).isEqualTo(0.75f);
    }

    @Test
    void createsPortraitDetailAndListWebpVariants() throws IOException {
        RestStopImageData result = processor.process(image("jpeg", 1200, 2400));

        assertDimensions(result.detailImageData(), 800, 1600);
        assertDimensions(result.listImageData(), 240, 480);
    }

    @Test
    void doesNotUpscaleSmallImages() throws IOException {
        RestStopImageData result = processor.process(image("jpeg", 320, 200));

        assertDimensions(result.detailImageData(), 320, 200);
        assertDimensions(result.listImageData(), 320, 200);
    }

    @Test
    void acceptsPngInput() throws IOException {
        RestStopImageData result = processor.process(image("png", 1200, 600));

        assertDimensions(result.detailImageData(), 1200, 600);
        assertDimensions(result.listImageData(), 480, 240);
    }

    @Test
    void wrapsIoExceptionFromReadingTheFile() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getInputStream()).thenThrow(new IOException("boom"));

        assertThatThrownBy(() -> processor.process(file))
                .isInstanceOf(InvalidRestStopImageException.class)
                .hasMessage("Unable to read image file");
    }

    @Test
    void rejectsAnEmptyFile() {
        MultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> processor.process(file)).isInstanceOf(InvalidRestStopImageException.class);
    }

    @Test
    void rejectsCorruptJpegData() {
        MultipartFile file = new MockMultipartFile("file", "corrupt.jpg", "image/jpeg", new byte[] {1, 2, 3});

        assertThatThrownBy(() -> processor.process(file)).isInstanceOf(InvalidRestStopImageException.class);
    }

    @Test
    void rejectsGifInput() throws IOException {
        MultipartFile file = image("gif", 100, 100);

        assertThatThrownBy(() -> processor.process(file)).isInstanceOf(InvalidRestStopImageException.class);
    }

    @Test
    void rejectsImagesOverThirtyMegapixelsBeforeDecoding() {
        MultipartFile file = new MockMultipartFile("file", "large.png", "image/png", minimalPng(6000, 5001));

        assertThatThrownBy(() -> processor.process(file))
                .isInstanceOf(InvalidRestStopImageException.class)
                .hasMessage("Image is too large");
    }

    private MultipartFile image(String format, int width, int height) throws IOException {
        int imageType = "png".equals(format) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage image = new BufferedImage(width, height, imageType);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        boolean written = ImageIO.write(image, format, output);
        if (!written) {
            throw new IllegalStateException("Unable to write test image");
        }

        return new MockMultipartFile("file", "source." + format, "image/" + format, output.toByteArray());
    }

    /**
     * IHDR/IEND의 CRC를 실제로 계산해 넣은, 픽셀 데이터 없는 최소 유효 PNG. 리더가 픽셀을
     * 디코딩하지 않고도 width/height 메타데이터만으로 검증을 통과하도록 만든다.
     */
    private byte[] minimalPng(int width, int height) {
        byte[] ihdrData = ByteBuffer.allocate(13)
                .putInt(width)
                .putInt(height)
                .put((byte) 8)
                .put((byte) 2)
                .put((byte) 0)
                .put((byte) 0)
                .put((byte) 0)
                .array();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.writeBytes(new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
        writeChunk(output, "IHDR", ihdrData);
        writeChunk(output, "IEND", new byte[0]);
        return output.toByteArray();
    }

    private void writeChunk(ByteArrayOutputStream output, String type, byte[] data) {
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        output.writeBytes(ByteBuffer.allocate(4).putInt(data.length).array());
        output.writeBytes(typeBytes);
        output.writeBytes(data);
        output.writeBytes(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }

    private void assertDimensions(byte[] data, int width, int height) {
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(data));
        } catch (IOException exception) {
            throw new AssertionError("Unable to read generated WebP", exception);
        }

        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isEqualTo(width);
        assertThat(image.getHeight()).isEqualTo(height);
    }

    private void assertWebp(byte[] data) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            assertThat(readers.hasNext()).isTrue();
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                assertThat(reader.getFormatName()).isEqualToIgnoringCase("webp");
            } finally {
                reader.dispose();
            }
        }
    }
}

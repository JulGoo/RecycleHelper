package kr.kro.barrierfree.recyclehelper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ImageUtils {

    public ByteBuffer preprocessImage(Image mediaImage, int inputWidth, int inputHeight) {
    // 1. Image (YUV_420_888 포맷)를 Bitmap으로 변환
        Bitmap bitmap = toBitmap(mediaImage);

        // 2. Bitmap 리사이즈
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true);

        // 3. Bitmap을 ByteBuffer로 변환
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(1 * 3 * inputWidth * inputHeight * 4); // 4는 float의 크기
        inputBuffer.order(ByteOrder.nativeOrder());

        int[] intValues = new int[inputWidth * inputHeight];
        resizedBitmap.getPixels(intValues, 0, inputWidth, 0, 0, inputWidth, inputHeight);

        // 4. 픽셀 데이터를 정규화 (0-1 범위) 및 ByteBuffer에 채움
        for (int pixel : intValues) {
            float r = ((pixel >> 16) & 0xFF) / 255.0f;
            float g = ((pixel >> 8) & 0xFF) / 255.0f;
            float b = (pixel & 0xFF) / 255.0f;
            inputBuffer.putFloat(r);
            inputBuffer.putFloat(g);
            inputBuffer.putFloat(b);
        }

        // 5. ByteBuffer 반환 (모델의 입력으로 사용)
        return inputBuffer;
    }


    // 이미지를 Bitmap으로 변환하는 메서드 (YUV_420_888 포맷)
    private Bitmap toBitmap(Image image) {
        byte[] nv21 = yuv420ToNv21(image);  // Image -> NV21 포맷으로 변환
        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, out);  // NV21 -> JPEG 변환
        byte[] jpegBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);  // JPEG -> Bitmap 변환
    }

    // YUV_420_888 이미지를 NV21 포맷으로 변환하는 메서드
    private byte[] yuv420ToNv21(Image image) {
        byte[] nv21;
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer(); // Y
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer(); // U
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer(); // V

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        nv21 = new byte[ySize + uSize + vSize];

        // Y 데이터 복사
        yBuffer.get(nv21, 0, ySize);

        // VU 순서로 NV21에 복사
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        return nv21;
    }


}


package kr.akotis.recyclehelper;

import android.Manifest;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.annotations.NotNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class VoiceSearchActivity extends AppCompatActivity {
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;
    private ImageView micImageView;
    private TextView statusTextView;
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;



    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_voice_search);

        micImageView = findViewById(R.id.iv_voice_search);
        statusTextView = findViewById(R.id.statusTextView);

        // 오디오 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }

        // SpeechRecognizer 초기화
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        // 음성인식 버튼 클릭 이벤트
        micImageView.setOnLongClickListener(view -> {
            if (!isListening) {
                startListening();
            }
            return true;
        });
        micImageView.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                stopListening();
            }
            return false;
        });
    }

    // 권한 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "오디오 권한이 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    //
    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR"); // 언어 설정
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override   // 말하기 시작할 준비되면 호출
            public void onReadyForSpeech(Bundle params) {
                //statusTextView.setText("음성인식 중...");
                vibrateOnClick();
                micImageView.setImageResource(R.drawable.voice_search_icon_on); // 활성화 상태 표시
            }

            @Override   // 말하기 시작했을때
            public void onBeginningOfSpeech() {}

            @Override   // 입력받는 소리의 크기
            public void onRmsChanged(float rmsdB) {}

            @Override   // 말을 시작하고 인식 된 단어 buffer 저장
            public void onBufferReceived(byte[] buffer) {}

            @Override   // 말하기 중지
            public void onEndOfSpeech() {
                statusTextView.setText("처리 중...");
            }

            @Override   // 네트워크 또는 인식 오류
            public void onError(int error) {
                @SuppressLint("SwitchIntDef")
                String message = switch (error) {
                    case SpeechRecognizer.ERROR_AUDIO -> "오디오 에러";
                    case SpeechRecognizer.ERROR_CLIENT -> "클라이언트 에러";
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "권한 없음";
                    case SpeechRecognizer.ERROR_NETWORK -> "네트워크 에러";
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "네트워크 타임아웃";
                    case SpeechRecognizer.ERROR_NO_MATCH -> "찾을 수 없음";
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Busy RECOGNIZER";
                    case SpeechRecognizer.ERROR_SERVER -> "서버가 에러";
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "시간초과";
                    default -> "알 수 없는 오류입니다.";
                };
                Toast.makeText(VoiceSearchActivity.this, message, Toast.LENGTH_SHORT).show();
                micImageView.setImageResource(R.drawable.voice_search_icon); // 비활성화 상태 표시
            }

            @Override   // 결과 처리
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    //statusTextView.setText(matches.get(0)); // 가장 정확한 결과 표시
                    fetchRecyclingInfo(matches.get(0));
                }
                //micImageView.setImageResource(R.drawable.ic_mic_inactive);
                //for(int i=0; i<matches.size(); i++) {
                //    statusTextView.setText(matches.get(i));
                //    Log.e("Answer : ", matches.get(i));
                //}

            }

            @Override   // 부분 인식 결과를 사용할 수 있을 때 호출
            public void onPartialResults(Bundle partialResults) {}

            @Override   // 향 후 이벤트를 추가하기 위해 예약
            public void onEvent(int eventType, Bundle params) {}
        });

        speechRecognizer.startListening(intent);
        isListening = true;
    }

    private void stopListening() {
        if (isListening) {
            speechRecognizer.stopListening();
            isListening = false;
            micImageView.setImageResource(R.drawable.voice_search_icon);
        }
    }

    private void vibrateOnClick() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }



    // OpenAI 분리배출 방법 요청
    private void fetchRecyclingInfo(String query) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.openai.com/v1/chat/completions";
        String apiKey = BuildConfig.API_KEY;;
        String moreprompt = "의 분리수거 방법을 알려주세요." +
                "만약 지금 말한 물체가 문장 혹은 물체가 아닌경우 '잘못된 정보입니다' 를 반환하세요." +
                "만약 옳바른 물체인경우 다음 형식과 같이 반환하세요." +
                "'ㅇㅇ은 종이류로 분리배출하여야 합니다.\n" +
                "분리배출을 위해서는 다음 과정을 따라주세요.\n\n" +
                "1. 라벨이 있을경우 제거합니다.\n" +
                "2. 부피를 최대한 줄여서 분리배출 합니다.\n" +
                "3. ....'";

        

        JSONObject requestBody = new JSONObject();
        JSONArray messages = new JSONArray();
        try {
            requestBody.put("model", "gpt-3.5-turbo");

            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", query + moreprompt));
            requestBody.put("messages", messages);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(requestBody.toString(), MediaType.parse("application/json")))
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e("ServerRequest", "서버 요청 실패(연결X)", e);
                runOnUiThread(() -> Toast.makeText(VoiceSearchActivity.this, "인터넷 연결에 실패하였습니다.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    String content = "NO MESSAGE";
                    Log.e("ServerRequest", responseData);

                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONArray choices = jsonObject.getJSONArray("choices");
                        JSONObject choice = choices.getJSONObject(0);
                        JSONObject message = choice.getJSONObject("message");
                        content = message.getString("content");
                    } catch (JSONException e) {
                        Log.e("JSON Parsing ERROR", "JSON 파싱 에러 " + responseData, e);
                    }

                    String finalContent = content;
                    runOnUiThread(() -> {
                        statusTextView.setText(finalContent);
                    });
                } else {
                    Log.e("ServerRequest", "응답 오류 " + response.code());
                    Log.e("ServerRequest", "응답 :  " + response.body().string());
                }
            }
        });
    }













    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

}
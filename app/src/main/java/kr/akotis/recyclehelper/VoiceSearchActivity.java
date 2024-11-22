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
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
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
import org.w3c.dom.Text;

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

public class VoiceSearchActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;
    private ImageButton micImageView;
    private TextView statusTextView;
    private TextView resultTextView1;
    private TextView resultTextView2;
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false;
    private TextToSpeech tts;





    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_voice_search);

        micImageView = findViewById(R.id.voiceButton);
        statusTextView = findViewById(R.id.statusTextView);
        resultTextView1 = findViewById(R.id.voice_result1);
        resultTextView2 = findViewById(R.id.voice_result2);
        tts = new TextToSpeech(this, this); //첫번째는 Context 두번째는 리스너



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
    

    // 음성인식 리스닝
    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ko-KR"); // 언어 설정
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override   // 말하기 시작할 준비되면 호출
            public void onReadyForSpeech(Bundle params) {
                tts.stop();
                statusTextView.setText("음성인식 중...");
                vibrateOnClick();
                //micImageView.setImageDrawable(R.drawable.voice_search_icon_on);
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
                micImageView.setImageResource(R.drawable.voice_search_icon_black); // 비활성화 상태 표시
            }

            @Override   // 결과 처리
            public void onResults(Bundle results) {
                statusTextView.setText("마이크를 눌러주세요.");
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    runOnUiThread(() -> {
                        statusTextView.setText(matches.get(0));
                        tts.stop();
                    });
                    fetchRecyclingInfo(matches.get(0));
                }
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
            micImageView.setImageResource(R.drawable.voice_search_icon_black);
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
        String systemprompt = "You are an expert in waste sorting and recycling according to South Korean regulations. Provide detailed and accurate guidance.";
        String moreprompt = "의 분리수거 방법을 알려주세요." +
                "만약 위에 입력된 단어가 문장 혹은 물체(쓰레기)가 아닌경우 '잘못된 정보입니다' 를 반환하세요.\n" +
                "예시) 집가고 싶다, 공기, 수학여행\n" +
                "만약 옳바른 물체 혹은 쓰레기인경우 다음 형식과 같이 반환하세요." +
                "'ㅇㅇ은 종이류로 분리배출하여야 합니다.\n" +
                "분리배출을 위해서는 다음 과정을 따라주세요.\n\n" +
                "1. 라벨이 있을경우 제거합니다.\n" +
                "2. 부피를 최대한 줄여서 분리배출 합니다.\n" +
                "3. ....'";

        String moreprompt2 = "chatgpt로 프롬포트를 작성할건데 입력한 단어가 \n" +
                "ㅇㅇ의 분리배출 방법 알려줘\n" +
                "에서 ㅇㅇ 부분에 들어가기 적합하거나 이상한 단어이면 \"적절한 물체가 아닙니다.\"라고 답해.\n" +
                "만약 적절한 문장이라면 해당 물건에 대한 분리배출 방법을 대한민국 기준으로 설명해줘.\n" +
                "만약 분리배출이 애매한 물건이거나, 적절한 규정이 없다면 일반쓰레기로 버려야한다고 반환해줘.\n" +
                "만약 분리배출 방법이 존재한다면 다음과 같은 형식으로 답변해줘.\n" +
                "\"\'ㅇㅇ\'은 (분리배출방법)으로 버려야 합니다.\"" +
                "을 출력하고, 아래 내용을 출력해." +
                "추가적으로 알아야될 분리배출 규칙이 있다면 1. 2. 3... 등 추가하여 출력해줘." +
                "이번 명령에 ㅇㅇ에 들어가는 단어는 \"" + query + "\"야";

        JSONObject requestBody = new JSONObject();
        JSONArray messages = new JSONArray();
        try {
            requestBody.put("model", "gpt-4o-mini");
            //requestBody.put("model", "gpt-3.5-turbo");

            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", systemprompt));

            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", moreprompt2));
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

                    // 결과값 문자열 나누기
                    int splitLine = content.indexOf("\n");
                    String one, two;
                    if (splitLine != -1) {
                        one = content.substring(0, splitLine).trim(); // 첫 번째 문장
                        two = content.substring(splitLine + 1).trim(); // 나머지 문장
                    } else {
                        // \n이 없을 경우 처리
                        one = "";
                        two = content;
                    }

                    String finalContent = content;
                    runOnUiThread(() -> {
                        //resultTextView1.setText(one);
                        //resultTextView2.setText(two);
                        resultTextView2.setText(finalContent);
                        speakOutNow();
                    });
                } else {
                    Log.e("ServerRequest", "응답 오류 " + response.code());
                    Log.e("ServerRequest", "응답 :  " + response.body().string());
                }
            }
        });
    }








    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int language = tts.setLanguage(Locale.KOREAN);

            if (language == TextToSpeech.LANG_MISSING_DATA || language == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "지원하지 않는 언어입니다.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "TTS에 실패하였습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void speakOutNow() {
        String text = resultTextView2.getText().toString();
        //tts.setPitch((float) 0.1); //음량
        //tts.setSpeechRate((float) 0.5); //재생속도
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }












    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

}
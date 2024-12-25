package kr.akotis.recyclehelper.community;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Community implements Parcelable {
    private String postId;
    private String title;
    private String content;
    private Long date;
    private String imgUrls;
    private String hashedPwd;
    private int report;
    private Map<String, Comment> comments;
    private String searchField; // 추가된 필드

    public Community() {
    }

    public Community(String postId, String title, String content, Long date, String imgUrls, String hashedPwd, int report, Map<String, Comment> comments) {
        this.postId = postId;  // postId 사용
        this.title = title;
        this.content = content;
        this.date = date;
        this.imgUrls = imgUrls;
        this.hashedPwd = hashedPwd; // 변경된 필드 사용
        this.report = report;
        this.comments = comments;
        this.searchField = generateSearchField(title, content); // title과 content를 합쳐 2-gram으로 searchField 설정
    }

    public String getPostId() {  // getter 변경
        return postId;
    }

    public void setPostId(String postId) {  // setter 변경
        this.postId = postId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }

    public String getImgUrls() {
        return imgUrls;
    }

    public void setImgUrls(String imgUrls) {
        this.imgUrls = imgUrls;
    }

    public String getHashedPwd() { // 변경된 Getter
        return hashedPwd;
    }

    public void setHashedPwd(String hashedPwd) { // 변경된 Setter
        this.hashedPwd = hashedPwd;
    }

    public int getReport() {
        return report;
    }

    public void setReport(int report) {
        this.report = report;
    }

    public Map<String, Comment> getComments() {
        return comments;
    }

    public void setComments(Map<String, Comment> comments) {
        this.comments = comments;
    }

    public String getSearchField() {
        return searchField;
    }

    // title과 content를 합쳐서 2-gram을 생성하는 메소드 (trim 및 연속 공백 처리)
    private String generateSearchField(String title, String content) {
        String combinedText = (title + " " + content).toLowerCase().trim().replaceAll("\\s+", " ");

        Set<String> nGrams = new HashSet<>();

        //2-gram 생성
        for (int i = 0; i < combinedText.length() - 1; i++) {
            String nGram = combinedText.substring(i, i + 2);   //2글자씩 자름
            nGrams.add(nGram);  //Set에 추가하여 중복 제거
        }

        return String.join(" ", nGrams);
    }

    protected Community(Parcel in) {
        postId = in.readString();  // postId 읽기
        title = in.readString();
        content = in.readString();
        date = in.readLong();
        imgUrls = in.readString();
        hashedPwd = in.readString(); // 변경된 필드
        report = in.readInt();
        comments = in.readHashMap(Comment.class.getClassLoader());
        searchField = in.readString(); // searchField 읽기
    }

    public static final Creator<Community> CREATOR = new Creator<Community>() {
        @Override
        public Community createFromParcel(Parcel in) {
            return new Community(in);
        }

        @Override
        public Community[] newArray(int size) {
            return new Community[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(postId);  // postId 저장
        dest.writeString(title);
        dest.writeString(content);
        dest.writeLong(date);
        dest.writeString(imgUrls);
        dest.writeString(hashedPwd); // 변경된 필드
        dest.writeInt(report);
        dest.writeMap(comments);
        dest.writeString(searchField); // searchField 저장
    }
}

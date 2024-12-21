package kr.akotis.recyclehelper.community;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Map;

public class Community implements Parcelable {
    private String id;
    private String title;
    private String content;
    private Long date;
    private String imgUrls;
    private String hashedPwd; // 필드 이름 변경
    private int report;
    private Map<String, Comment> comments;

    public Community() {}

    public Community(String id, String title, String content, Long date, String imgUrls, String hashedPwd, int report, Map<String, Comment> comments) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.date = date;
        this.imgUrls = imgUrls;
        this.hashedPwd = hashedPwd; // 변경된 필드 사용
        this.report = report;
        this.comments = comments;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    protected Community(Parcel in) {
        id = in.readString();
        title = in.readString();
        content = in.readString();
        date = in.readLong();
        imgUrls = in.readString();
        hashedPwd = in.readString(); // 변경된 필드
        report = in.readInt();
        comments = in.readHashMap(Comment.class.getClassLoader());
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
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(content);
        dest.writeLong(date);
        dest.writeString(imgUrls);
        dest.writeString(hashedPwd); // 변경된 필드
        dest.writeInt(report);
        dest.writeMap(comments);
    }
}

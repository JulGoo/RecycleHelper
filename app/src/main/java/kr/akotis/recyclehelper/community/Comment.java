package kr.akotis.recyclehelper.community;

import android.os.Parcel;
import android.os.Parcelable;

public class Comment implements Parcelable {
    private String commentId;  // id를 commentId로 변경
    private String content;
    private long date;
    private String pwd;
    private int report;

    public Comment() {}

    public Comment(String commentId, String content, long date, String pwd, int report) {
        this.commentId = commentId;  // commentId 사용
        this.content = content;
        this.date = date;
        this.pwd = pwd;
        this.report = report;
    }

    // commentId getter, setter 추가
    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public int getReport() {
        return report;
    }

    public void setReport(int report) {
        this.report = report;
    }

    // Parcelable 구현
    protected Comment(Parcel in) {
        commentId = in.readString();  // commentId 읽기
        content = in.readString();
        date = in.readLong();
        pwd = in.readString();
        report = in.readInt();
    }

    public static final Creator<Comment> CREATOR = new Creator<Comment>() {
        @Override
        public Comment createFromParcel(Parcel in) {
            return new Comment(in);
        }

        @Override
        public Comment[] newArray(int size) {
            return new Comment[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(commentId);  // commentId 저장
        dest.writeString(content);
        dest.writeLong(date);
        dest.writeString(pwd);
        dest.writeInt(report);
    }
}

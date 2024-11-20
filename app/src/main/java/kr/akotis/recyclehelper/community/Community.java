package kr.akotis.recyclehelper.community;

import android.os.Parcel;
import android.os.Parcelable;

public class Community implements Parcelable {
    private String title;
    private String content;
    private String date;
    private String imgUrls;
    private int pwd;
    private int report;

    public Community() {}

    public Community(String title, String content, String date, String imgUrls, int pwd, int report) {
        this.title = title;
        this.content = content;
        this.date = date;
        this.imgUrls = imgUrls;
        this.pwd = pwd;
        this.report = report;
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getImgUrls() {
        return imgUrls;
    }

    public void setImgUrls(String imgUrls) {
        this.imgUrls = imgUrls;
    }

    public int getPwd() {
        return pwd;
    }

    public void setPwd(int pwd) {
        this.pwd = pwd;
    }

    public int getReport() {
        return report;
    }

    public void setReport(int report) {
        this.report = report;
    }

    protected Community(Parcel in){
        title = in.readString();
        content = in.readString();
        date = in.readString();
        imgUrls = in.readString();
        pwd = in.readInt();
        report = in.readInt();
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
        dest.writeString(title);
        dest.writeString(content);
        dest.writeString(date);
        dest.writeString(imgUrls);
        dest.writeInt(pwd);
        dest.writeInt(report);
    }
}

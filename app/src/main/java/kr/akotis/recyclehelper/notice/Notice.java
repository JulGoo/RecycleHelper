package kr.akotis.recyclehelper.notice;

public class Notice {
    private String title;
    private String content;
    private String date;
    private String imgUrl;

    public Notice() {}

    public Notice(String title, String content, String date, String imgUrl){
        this.title = title;
        this.content = content;
        this.date = date;
        this.imgUrl = imgUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getDate() {
        return date;
    }

    public String getImgUrl() {
        return imgUrl;
    }
}

package kr.akotis.recyclehelper.community;

public class Comment {
    private String content;
    private long date;
    private int pwd;
    private int report;

    public Comment() {
    }

    public Comment(String content, long date, int pwd, int report) {
        this.content = content;
        this.date = date;
        this.pwd = pwd;
        this.report = report;
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
}
package org.example.Parser;

public class Record {
    private String title;
    private String text;
    private String src;

    public Record(String title, String text, String src) {
        this.setTitle(title);
        this.setText(text);
        this.setSrc(src);
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    @Override
    public String toString(){
        return this.title + " " + this.text + " " + this.src;
    }
}

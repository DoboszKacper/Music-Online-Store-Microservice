package Models;

public class Song {
    private int id;
    private String songName;

    public String getSongName() {
        return songName;
    }

    public Song(String songName) {
        this.songName = songName;
    }

    public Song() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }
}

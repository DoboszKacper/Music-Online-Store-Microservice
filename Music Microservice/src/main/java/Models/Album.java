package Models;

import java.util.List;

public class Album {
    private int id;
    private String albumName;
    private List<Song> songList;

    public Album(String albumName, List<Song> songList) {
        this.albumName = albumName;
        this.songList = songList;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Album() {
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public List<Song> getSongList() {
        return songList;
    }

    public void setSongList(List<Song> songList) {
        this.songList = songList;
    }
}

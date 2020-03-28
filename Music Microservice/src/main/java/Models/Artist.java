package Models;

import java.util.List;

public class Artist {
    private int id;
    private String name;
    private List<Album> albumsList;

    public String getName() {
        return name;
    }

    public Artist(String name, List<Album> albumsList) {
        this.name = name;
        this.albumsList = albumsList;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Artist() {
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Album> getAlbumsList() {
        return albumsList;
    }

    public void setAlbumsList(List<Album> albumsList) {
        this.albumsList = albumsList;
    }
}

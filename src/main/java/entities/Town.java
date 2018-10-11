package entities;

import annotations.Column;
import annotations.Entity;
import annotations.PrimaryKey;

@Entity(name = "towns")
public class Town {

    @PrimaryKey
    @Column(name = "id")
    int id;

    @Column(name = "name")
    String name;

    public Town() {
    }

    public Town(String name) {
        this.name = name;
    }
}

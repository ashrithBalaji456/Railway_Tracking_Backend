package com.example.railtracker.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "favorites", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "favorite_type", "item_code"})
})
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "favorite_type", nullable = false, length = 20)
    private String favoriteType; // "TRAIN" or "STATION"

    @Column(name = "item_code", nullable = false, length = 50)
    private String itemCode; // train number or station code

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName; // train name or station name

    public Favorite() {
    }

    public Favorite(User user, String favoriteType, String itemCode, String itemName) {
        this.user = user;
        this.favoriteType = favoriteType;
        this.itemCode = itemCode;
        this.itemName = itemName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getFavoriteType() {
        return favoriteType;
    }

    public void setFavoriteType(String favoriteType) {
        this.favoriteType = favoriteType;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
}

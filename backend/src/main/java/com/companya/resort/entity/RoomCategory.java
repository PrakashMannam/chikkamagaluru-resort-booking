package com.companya.resort.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "room_categories")
public class RoomCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "total_physical_keys", nullable = false)
    private Integer totalPhysicalKeys;

    @Column(name = "max_adults", nullable = false)
    private Integer maxAdults;

    @Column(name = "max_children", nullable = false)
    private Integer maxChildren;

    @Column(name = "base_description", columnDefinition = "TEXT")
    private String baseDescription;

    @Column(name = "photo_urls_json", columnDefinition = "JSON")
    private String photoUrlsJson;

    public RoomCategory() {
    }

    public RoomCategory(Long id, String code, String name, Integer totalPhysicalKeys,
                        Integer maxAdults, Integer maxChildren, String baseDescription, String photoUrlsJson) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.totalPhysicalKeys = totalPhysicalKeys;
        this.maxAdults = maxAdults;
        this.maxChildren = maxChildren;
        this.baseDescription = baseDescription;
        this.photoUrlsJson = photoUrlsJson;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getTotalPhysicalKeys() {
        return totalPhysicalKeys;
    }

    public void setTotalPhysicalKeys(Integer totalPhysicalKeys) {
        this.totalPhysicalKeys = totalPhysicalKeys;
    }

    public Integer getMaxAdults() {
        return maxAdults;
    }

    public void setMaxAdults(Integer maxAdults) {
        this.maxAdults = maxAdults;
    }

    public Integer getMaxChildren() {
        return maxChildren;
    }

    public void setMaxChildren(Integer maxChildren) {
        this.maxChildren = maxChildren;
    }

    public String getBaseDescription() {
        return baseDescription;
    }

    public void setBaseDescription(String baseDescription) {
        this.baseDescription = baseDescription;
    }

    public String getPhotoUrlsJson() {
        return photoUrlsJson;
    }

    public void setPhotoUrlsJson(String photoUrlsJson) {
        this.photoUrlsJson = photoUrlsJson;
    }
}

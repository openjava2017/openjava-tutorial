package org.openjava.mongo.pojo;

public class Feature {
    private Integer height;
    private Integer weight;

    public static Feature of(int height, int weight) {
        Feature feature = new Feature();
        feature.height = height;
        feature.weight = weight;
        return feature;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }
}

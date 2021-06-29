package com.example.mapapp;

public class ModelAPI {
    private int cluster;
    private String kecamatan;

    public ModelAPI(int cluster, String kecamatan){
        this.cluster = cluster;
        this.kecamatan = kecamatan;
    }

    public int getCluster() {
        return cluster;
    }

    public String getKecamatan() {
        return kecamatan;
    }

    @Override
    public String toString() {
        return "{" +
                "\"cluster\":" + cluster +
                ",\"kecamatan\":\"" + kecamatan + "\"" + "}";
    }

}

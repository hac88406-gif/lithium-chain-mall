package com.greenchain.entity;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessNode {

    private String id;

    private String name;

    private String type;

    private String description;

    private String workshop;

    private Integer duration;

    private List<ProcessNode> nextSteps = new ArrayList<>();

    public ProcessNode() {
    }

    public ProcessNode(String id, String name, String type, String description, String workshop, Integer duration) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
        this.workshop = workshop;
        this.duration = duration;
    }
}
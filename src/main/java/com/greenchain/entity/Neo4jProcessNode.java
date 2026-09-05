package com.greenchain.entity;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

@Data
@Node("ProcessNode")
public class Neo4jProcessNode {

    @Id
    @GeneratedValue
    private Long neo4jId;

    private String id;

    private String name;

    private String type;

    private String description;

    private String workshop;

    private Integer duration;

    @Relationship(type = "NEXT_STEP", direction = Relationship.Direction.OUTGOING)
    private List<Neo4jProcessNode> nextSteps = new ArrayList<>();
}
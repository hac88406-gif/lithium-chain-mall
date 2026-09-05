package com.greenchain.repository;

import com.greenchain.entity.ProcessNode;

import java.util.List;
import java.util.Optional;

public interface ProcessNodeRepository {

    Optional<ProcessNode> findById(String id);

    List<ProcessNode> findAll();

    List<ProcessNode> findByWorkshop(String workshop);

    List<ProcessNode> findFullProcessChain();

    List<ProcessNode> findSubsequentNodes(String id);

    long countNodes();

    long countRelationships();
}
package com.mAtiehDev.jobtracker.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mAtiehDev.jobtracker.model.GmailToken;

public interface GmailTokenRepository extends JpaRepository<GmailToken, String> {}


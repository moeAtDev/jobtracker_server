//tables creation 
CREATE TABLE jobtracker_data.user_table (
     user_id VARCHAR(150) PRIMARY KEY,
    user_name VARCHAR(100) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL, 
    email_address VARCHAR(200) NOT NULL, 
    user_header VARCHAR(150),
    user_city VARCHAR(150) ,
    user_country VARCHAR(150) ,
    password  VARCHAR(200) NOT NULL,
    summary TEXT 
);


CREATE TABLE jobtracker_data.user_experience (
     job_id VARCHAR(150) PRIMARY KEY,
     user_id VARCHAR(100) NOT NULL,
    user_name VARCHAR(100) NOT NULL,
    job_title VARCHAR(100) NOT NULL,
    job_description TEXT  NOT NULL,
    job_type VARCHAR(100) NOT NULL,
    company_name VARCHAR(100) NOT NULL,
    company_city VARCHAR(100) NOT NULL,
    company_country VARCHAR(100) NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL
  
);

CREATE TABLE jobtracker_data.user_education (
     Id VARCHAR(150) PRIMARY KEY,
     user_id VARCHAR(100) NOT NULL,
    user_name VARCHAR(100) NOT NULL,
    degree_type VARCHAR(200) NOT NULL,
    major VARCHAR(200) NOT NULL,
    university_name VARCHAR(200) NOT NULL,
    country VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL
  
);


CREATE TABLE jobtracker_data.gmail_tokens (
     Id VARCHAR(150) PRIMARY KEY,
     user_id VARCHAR(100) NOT NULL,
    user_name VARCHAR(100) NOT NULL,
    skill_name VARCHAR(200) NOT NULL
   
  
);


//analysis tables
/'user_id VARCHAR NOT NULL REFERENCES jobtracker_data.users(user_id) ON DELETE CASCADE,
this ensure that if user with user_id linked to this analysis this analysis will delete autoamtically
'/

CREATE TABLE jobtracker_data.analysis (
    analysis_id VARCHAR PRIMARY KEY,
    user_id VARCHAR NOT NULL REFERENCES jobtracker_data.user_table(user_id) ON DELETE CASCADE,
    analysis_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    match_score NUMERIC(5,2),
    resume_raw_text TEXT,
    job_description_raw_text TEXT,
     file_name VARCHAR,
     job_title VARCHAR,
     comapny_name VARCHAR
    
    
);



-- Missing skills
CREATE TABLE jobtracker_data.missing_skills (
    missing_skill_id VARCHAR PRIMARY KEY,
    analysis_id VARCHAR NOT NULL REFERENCES jobtracker_data.analysis(analysis_id) ON DELETE CASCADE,
    skill_name TEXT NOT NULL
);

-- Skills needing improvement
CREATE TABLE jobtracker_data.skills_to_improve (
    improve_skill_id VARCHAR PRIMARY KEY,
    analysis_id VARCHAR NOT NULL REFERENCES jobtracker_data.analysis(analysis_id) ON DELETE CASCADE,
    skill_name TEXT NOT NULL,
    reason TEXT
);


CREATE TABLE jobtracker_data.recommendations (
    recommendation_id VARCHAR PRIMARY KEY,
    analysis_id VARCHAR NOT NULL REFERENCES jobtracker_data.analysis(analysis_id) ON DELETE CASCADE,
    recommendation_text TEXT
);

--tracked jobs 
CREATE TABLE jobtracker_data.gmail_tokens (
     user_id VARCHAR(150) PRIMARY KEY,
    access_token VARCHAR(255) NOT NULL,
    refresh_token VARCHAR(255) NOT NULL,
	expiry_time TIMESTAMP,
	last_modified_time TIMESTAMP

);

CREATE TABLE jobtracker_data.tracked_applications (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(255),
    job_title VARCHAR(255),
    company VARCHAR(255),
    application_source VARCHAR(100),
    applied_at TIMESTAMP,
    email_snippet TEXT,
    job_description TEXT,
    resume_text TEXT,
    resume_file_data bytea,
    resume_file_name VARCHAR(255),
    resume_file_type VARCHAR(100)

);

CREATE TABLE IF NOT EXISTS jobtracker_data.recommended_jobs (
  id           bigserial PRIMARY KEY,
  user_id      varchar NOT NULL,
  title        varchar,
  company      varchar,
  location     varchar,
  snippet      text,
  url          varchar(1024),
  salary       varchar,
  fetched_at   timestamptz
);



//seq creation
CREATE SEQUENCE jobtracker_data.user_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE jobtracker_data.education_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE jobtracker_data.experience_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE jobtracker_data.skill_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE jobtracker_data.analysis_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE jobtracker_data.missing_skill_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE jobtracker_data.skill_improve_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE jobtracker_data.recommendation_seq START WITH 1 INCREMENT BY 1;

--index for recommaded job 
CREATE INDEX IF NOT EXISTS idx_recommended_jobs_user
  ON jobtracker_data.recommended_jobs (user_id);



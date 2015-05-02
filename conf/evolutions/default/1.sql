# --- !Ups
CREATE TABLE Election(
    id serial PRIMARY KEY,
    name text NOT NULL,
    description text NOT NULL DEFAULT ''
);

CREATE TABLE Candidate(
    id serial PRIMARY KEY,
    electionId integer NOT NULL,
    name text NOT NULL
);

ALTER TABLE Candidate ADD CONSTRAINT fk_Candidate_electionId FOREIGN KEY (electionId) REFERENCES Election (id) MATCH FULL;

# --- !Downs
ALTER TABLE Candidate DROP CONSTRAINT fk_Candidate_electionId;
DROP TABLE Candidate;
DROP TABLE Election;

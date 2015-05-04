# --- !Ups
CREATE TABLE ElectionCount(
    id serial PRIMARY KEY,
    electionId int NOT NULL,
    seats int NOT NULL
);

CREATE TABLE ElectionCountDetail(
    id serial PRIMARY KEY,
    countId int NOT NULL,
    round smallint NOT NULL,
    candidateId int NOT NULL,
    isElected boolean NOT NULL,
    voteCount int NOT NULL
);

ALTER TABLE ElectionCount ADD CONSTRAINT fk_ElectionCount_electionId FOREIGN KEY (electionId) REFERENCES Election (id) MATCH FULL;
ALTER TABLE ElectionCountDetail ADD CONSTRAINT fk_ElectionCountDetail_countId FOREIGN KEY (countId) REFERENCES ElectionCount (id) MATCH FULL;
ALTER TABLE ElectionCountDetail ADD CONSTRAINT fk_ElectionCountDetail_candidateId FOREIGN KEY (candidateId) REFERENCES Candidate (id) MATCH FULL;

# --- !Downs
ALTER TABLE ElectionCountDetail DROP CONSTRAINT fk_ElectionCountDetail_candidateId;
ALTER TABLE ElectionCountDetail DROP CONSTRAINT fk_ElectionCountDetail_countId;
DROP TABLE ElectionCountDetail;
ALTER TABLE ElectionCount DROP CONSTRAINT fk_ElectionCount_electionId;
DROP TABLE ElectionCount;
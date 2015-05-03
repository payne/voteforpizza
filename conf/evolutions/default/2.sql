# --- !Ups
CREATE TABLE Ballot(
    id serial PRIMARY KEY,
    name text NOT NULL
);

CREATE TABLE Preference(
    id serial PRIMARY KEY,
    ballotId integer NOT NULL,
    candidateId integer NOT NULL,
    rank smallint NOT NULL
);

ALTER TABLE Preference ADD CONSTRAINT fk_Preference_ballotId FOREIGN KEY (ballotId) REFERENCES Ballot (id) MATCH FULL;
ALTER TABLE Preference ADD CONSTRAINT fk_Preference_candidateId FOREIGN KEY (candidateId) REFERENCES Candidate (id) MATCH FULL;

# --- !Downs
ALTER TABLE Ballot DROP CONSTRAINT fk_Preference_ballotId;
ALTER TABLE Ballot DROP CONSTRAINT fk_Preference_candidateId;
DROP TABLE Preference;
DROP TABLE Ballot;

# --- !Ups
DELETE FROM Preference;
DELETE FROM Ballot;
ALTER TABLE Ballot ADD electionId INT NOT NULL;
ALTER TABLE Ballot ADD CONSTRAINT fk_Ballot_electionId FOREIGN KEY (electionId) REFERENCES Election (id) MATCH FULL;

# --- !Downs
ALTER TABLE Ballot DROP CONSTRAINT fk_Ballot_electionId;
ALTER TABLE Ballot DROP COLUMN electionId;

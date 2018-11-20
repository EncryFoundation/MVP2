CREATE TABLE accounts(
  public_key TEXT PRIMARY KEY,
  nonce BIGINT NOT NULL
);

CREATE TABLE accounts_data(
  public_key TEXT REFERENCES accounts(public_key),
  number_in_account INT NOT NULL,
  data BYTEA,
  PRIMARY KEY (public_key, number_in_account)
);

CREATE table blocks(
  is_microblock BOOLEAN NOT NULL,
  height BIGINT NOT NULL,
  timestamp BIGINT NOT NULL,
  previous_key_block_hash BYTEA,
  current_block_hash TEXT PRIMARY KEY,
  data BYTEA,
  prev_microblock BYTEA
);

CREATE TABLE transactions(
  public_key TEXT,
  timestamp BIGINT NOT NULL,
  nonce BIGINT NOT NULL,
  signature BYTEA,
  data BYTEA,
  block_hash TEXT REFERENCES blocks(current_block_hash),
  PRIMARY KEY (public_key, nonce, signature)
);
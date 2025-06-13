# This script takes dump of nom_rdbms_data_db_public_ressurs-dev and nom_rdbms_data_db_public_ressurs-prod
# Then finds the navident-personident entry that has null ident_type on dev, abd the assign ident_type from prod
# Results a new CSV file -  ready for import via ImportNavIdenter@importFromReader

import pandas as pd

# Define column names
columns = ['personident', 'navident', 'ident_type']

# Read the CSVs without headers
dev_df = pd.read_csv("nom_rdbms_data_db_public_ressurs-dev.csv", header=None, names=columns)
prod_df = pd.read_csv("nom_rdbms_data_db_public_ressurs-prod.csv", header=None, names=columns)

print(f"Total entries in orig dev file: {len(dev_df)}")
print(f"Total entries in orig prod file: {len(prod_df)}")

# Remove duplicate rows
dev_df = dev_df.drop_duplicates()
prod_df = prod_df.drop_duplicates()

# Merge: keep personident and navident from dev, ident_type from prod
merged_df = dev_df[['personident', 'navident']].merge(
    prod_df[['personident', 'ident_type']],
    on='personident',
    how='left'
)

# Remove rows with missing ident_type
merged_df = merged_df.dropna(subset=['ident_type'])

# Swap personident and navident columns
merged_df_columns = merged_df.columns.tolist()
merged_df_columns[0], merged_df_columns[1] = merged_df_columns[1], merged_df_columns[0]
merged_df = merged_df[merged_df_columns]

# Remove header and Save result
merged_df.to_csv("nom_rdbms_data_db_public_ressurs-merged.csv", index=False, header=False)

# Print count of rows
print(f"Total entries in dev file: {len(dev_df)}")
print(f"Total entries in prod file: {len(prod_df)}")
print(f"Total entries in merged file: {len(merged_df)}")

# Find and Remove duplicates
duplicates = merged_df[merged_df.duplicated(subset=merged_df.columns[0], keep=False)]
print(f"Duplicates entries in merged file: {duplicates}")
merged_df = merged_df.drop_duplicates()
print(f"Total entries in final merged file: {len(merged_df)}")


# This script reads an Excel file and exports selected columns to a JSON file.
# The Json data can then be used as input to a REST API for eks /api/admin/manuelleEvtsrcMeldinger/oppdatereTelefonnummerRessurs.


import pandas as pd

# Load the Excel file
# excel_file = 'Aktive medarb med direktenr og ikke mobilnr 20250106.xls'
# col1 = 'NAV-ID'
# col2 = 'Dir tlf_FINAL'

excel_file = 'Eksterne med mobilnr satt og ingen direktenr 20250106.xls'
col1 = 'NAV-ID'
col2 = 'Dir tlf FINAL'

sheet_name = 'adhoc_list_table'

# Read the Excel file
df = pd.read_excel(excel_file, sheet_name=sheet_name)

# Select specific columns and map to json keys
columns_to_export = [col1, col2] 
selected_df = df[columns_to_export]
new_column_names = {
    col1: 'navident',   
    col2: 'telefonummer'
}
selected_df.rename(columns=new_column_names, inplace=True)

# Save to JSON file
json_file = 'data.json'  
selected_df.to_json(json_file, orient='records', indent=4)

print(f"Selected columns saved to {json_file}")

# Print the total number of entries
num_entries = len(selected_df)
print(f"Number of entries: {num_entries}")


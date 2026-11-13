import json
import os

deployments = json.loads(os.environ['ADDITIONAL_DEV_DEPLOYMENTS'])
if not isinstance(deployments, list):
    raise ValueError('ADDITIONAL_DEV_DEPLOYMENTS must be a JSON array')
with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
    output.write(f'deployments={json.dumps(deployments, separators=(",", ":"))}\n')

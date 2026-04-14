import urllib.request
import json
import json

def test_osv_api():
    print("--- Testing Feature 3: OSV API ---")
    url = "https://api.osv.dev/v1/querybatch"
    payload = {
        "queries": [
            {
                "package": {"name": "react", "ecosystem": "npm"},
                "version": "15.0.0"
            }
        ]
    }
    
    data = json.dumps(payload).encode('utf-8')
    req = urllib.request.Request(url, data=data, headers={'Content-Type': 'application/json'})
    
    try:
        with urllib.request.urlopen(req) as response:
            result = json.loads(response.read().decode())
            vulns = result.get('results', [])[0].get('vulns', [])
            print(f"OSV API returned {len(vulns)} vulnerabilities for react@15.0.0.")
            if len(vulns) > 0:
                print(f"Sample Vulnerability: {vulns[0].get('summary', 'No summary')}")
            print("Feature 3 OSV API mapping: SUCCESS\n")
    except Exception as e:
        print(f"OSV API logic FAILED: {e}\n")

def test_nvd_api():
    print("--- Testing Feature 2: NVD API ---")
    # Using CWE-79 (Cross-site Scripting)
    url = "https://services.nvd.nist.gov/rest/json/cves/2.0?cweId=CWE-79&resultsPerPage=1"
    
    req = urllib.request.Request(url)
    
    try:
        with urllib.request.urlopen(req) as response:
            result = json.loads(response.read().decode())
            vulns = result.get('vulnerabilities', [])
            print(f"NVD API returned {len(vulns)} CVE matches for CWE-79.")
            if len(vulns) > 0:
                cve = vulns[0].get('cve', {})
                print(f"Sample CVE Extracted: {cve.get('id')}")
            print("Feature 2 NVD API mapping: SUCCESS\n")
    except Exception as e:
        print(f"NVD API FAILED: {e}\n")

if __name__ == "__main__":
    test_osv_api()
    test_nvd_api()

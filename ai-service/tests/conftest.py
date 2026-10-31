"""
Pytest configuration for TalentMatch AI Service
"""

import os
import sys

# Ensure app package is importable
service_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
if service_dir not in sys.path:
    sys.path.insert(0, service_dir)

os.environ["AI_SERVICE_KEY"] = "test-secret-key-12345"
os.environ["USE_TF"] = "0"
os.environ["TF_ENABLE_ONEDNN_OPTS"] = "0"

"""健康检查和基础端点测试"""
import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)


def test_health_check():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert data["service"] == "moxi-agent"


def test_metrics_endpoint():
    response = client.get("/metrics")
    assert response.status_code == 200
    data = response.json()
    assert "total_requests" in data


def test_traces_endpoint():
    response = client.get("/traces")
    assert response.status_code == 200
    data = response.json()
    assert "traces" in data
    assert "count" in data


def test_traces_with_limit():
    response = client.get("/traces?limit=5")
    assert response.status_code == 200

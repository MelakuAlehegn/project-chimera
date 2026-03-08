# Technical Specification

## TrendFetcher API

Input:
{
  "platform": "tiktok",
  "category": "fitness"
}

Output:
{
  "trends": [
    {
      "topic": "morning workout routine",
      "engagementScore": 0.89
    }
  ]
}

## Database Schema

Tables:

Trends
- id
- platform
- topic
- engagement_score

Videos
- id
- trend_id
- script
- publish_status
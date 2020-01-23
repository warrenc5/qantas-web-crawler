Feature: test a crawling

Background:

Scenario: Basic crawling

    Given path '/'
    And form field url = "https://www.qantasmoney.com/some" 
    When method post
    Then status 200
#TODO response parsing here
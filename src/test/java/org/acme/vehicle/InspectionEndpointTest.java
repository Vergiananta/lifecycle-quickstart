package org.acme.vehicle;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InspectionEndpointTest {

    @Test
    @Order(1)
    public void testGetVehicles_E1() {
        given()
                .when().get("/api/v1/vehicles")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.size()", equalTo(20))
                .body("data[0].id", equalTo(1))
                .body("data[0].plate_number", notNullValue());
    }

    @Test
    @Order(2)
    public void testCreateInspectionDraft_E2() {
        Map<String, Object> body = Map.of(
                "vehicle_id", 1,
                "inspector_name", "Budi",
                "has_key", true,
                "items", List.of(
                        Map.of("point_code", "EXT_FRONT", "condition", "GOOD")
                )
        );

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/api/v1/inspections")
                .then()
                .statusCode(201)
                .body("success", equalTo(true))
                .body("data.status", equalTo("DRAFT"))
                .body("data.version", equalTo(1))
                .body("data.inspector_name", equalTo("Budi"));
    }

    @Test
    @Order(3)
    public void testRuleR2_NoKey_EngineNotInspectable() {
        // Without key, ENGINE points must be NOT_INSPECTABLE
        Map<String, Object> body = Map.of(
                "vehicle_id", 1,
                "inspector_name", "Budi",
                "has_key", false,
                "items", List.of(
                        Map.of("point_code", "ENG_STARTUP", "condition", "GOOD") // Invalid: should be NOT_INSPECTABLE
                )
        );

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/api/v1/inspections")
                .then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("errors", not(empty()));
    }

    @Test
    @Order(4)
    public void testRuleR3_MajorDamageRequiresPhoto() {
        Map<String, Object> body = Map.of(
                "vehicle_id", 1,
                "inspector_name", "Budi",
                "has_key", true,
                "items", List.of(
                        Map.of("point_code", "EXT_FRONT", "condition", "MAJOR_DAMAGE", "photo_refs", List.of()) // Invalid: empty photo_refs
                )
        );

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/api/v1/inspections")
                .then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("errors", not(empty()));
    }

    @Test
    @Order(5)
    public void testRuleR4_IllegalTransition() {
        // First create a DRAFT
        Map<String, Object> body = Map.of(
                "vehicle_id", 1,
                "inspector_name", "Budi",
                "has_key", true,
                "items", List.of(
                        Map.of("point_code", "EXT_FRONT", "condition", "GOOD")
                )
        );

        Number id = given()
                .contentType(ContentType.JSON)
                .body(body)
                .when().post("/api/v1/inspections")
                .then()
                .statusCode(201)
                .extract().path("data.id");

        // Try illegal transition: DRAFT -> APPROVED (must be 409)
        Map<String, Object> transitionBody = Map.of(
                "to", "APPROVED",
                "actor", "Supervisor",
                "note", "Attempt direct approval"
        );

        given()
                .contentType(ContentType.JSON)
                .body(transitionBody)
                .when().post("/api/v1/inspections/" + id + "/transition")
                .then()
                .statusCode(409)
                .body("success", equalTo(false))
                .body("errors[0].field", equalTo("to"));
    }
}


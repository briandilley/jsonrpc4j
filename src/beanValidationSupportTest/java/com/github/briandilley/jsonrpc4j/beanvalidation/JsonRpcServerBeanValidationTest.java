package com.github.briandilley.jsonrpc4j.beanvalidation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.easymock.MockType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.googlecode.jsonrpc4j.ErrorResolver;
import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
import com.googlecode.jsonrpc4j.JsonRpcInterceptor;
import com.googlecode.jsonrpc4j.JsonRpcParam;
import com.googlecode.jsonrpc4j.JsonRpcServer;

import static com.googlecode.jsonrpc4j.JsonRpcBasicServer.RESULT;
import static com.googlecode.jsonrpc4j.util.Util.*;
import static org.junit.Assert.*;

@RunWith(EasyMockRunner.class)
public class JsonRpcServerBeanValidationTest {
    @Mock(type = MockType.NICE)
    private ServiceInterfaceWithBeanValidationAnnotations mockService;

    private ByteArrayOutputStream byteArrayOutputStream;
    private JsonRpcBasicServer jsonRpcServer;
    private ObjectMapper objectMapper;
    private TestBeanValidationJsonRpcInterceptor testBeanValidationJsonRpcInterceptor;

    @Before
    public void setup() {
        byteArrayOutputStream = new ByteArrayOutputStream();
        objectMapper = new ObjectMapper();
        jsonRpcServer = new JsonRpcBasicServer(objectMapper, mockService, ServiceInterfaceWithBeanValidationAnnotations.class);
        List<JsonRpcInterceptor> interceptors = new ArrayList<>(1);
        testBeanValidationJsonRpcInterceptor = new TestBeanValidationJsonRpcInterceptor();
        interceptors.add(testBeanValidationJsonRpcInterceptor);
        jsonRpcServer.setInterceptorList(interceptors);
    }

    // annotate necessary classes

    class User {
        @Positive
        final long id;

        @NotNull
        @Size(min = 3, max = 255)
        final String userName;

        @NotNull
        @Size(min = 12)
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=]).{12,}$")
        final String password;

        public User(long id, String userName, String password) {
            this.id = id;
            this.userName = userName;
            this.password = password;
        }
    }

    interface UserService {
        @Valid User createUser(
            @JsonRpcParam("theUserName")
            @NotNull
            @Size(min = 3, max = 255)
            String userName,

            @JsonRpcParam("thePassword")
            @NotNull
            @Size(min = 12)
            @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=]).{12,}$",
                message = "Password must be at least 12 characters long and contain "
            )
            String password
        );
    }

    class UserServiceImpl implements UserService{
        @Override
        public User createUser(String userName, String password) {
            return new User(1L, userName, password);
        }
    }

    @Test
    public void validationExampleFromTheReadmeDoc() {
        UserService userService = new UserServiceImpl();

// create the jsonRpcServer
        JsonRpcServer jsonRpcServer = new JsonRpcServer(userService, UserService.class);

// create the bean validation interceptor with the default jakarta.validation.ValidatorFactory
        JsonRpcInterceptor beanValidationJsonRpcInterceptor = new BeanValidationJsonRpcInterceptor();

// or create with existing ValidatorFactory
// ValidatorFactory validatorFactory = getOrCreateValidatorFactory();
// JsonRpcInterceptor beanValidationJsonRpcInterceptor = new BeanValidationJsonRpcInterceptor(validatorFactory);


// Add validation interceptor
        jsonRpcServer.getInterceptorList().add(beanValidationJsonRpcInterceptor);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int errorCode;
        try {
            errorCode = jsonRpcServer.handleRequest(
                new ByteArrayInputStream(
                    (
                        "{\"jsonrpc\": \"2.0\",  \"id\": 54321, \"method\": \"createUser\", "
                            + "\"params\": {\"theUserName\": \"me\", \"thePassword\": \"123456\"}"
                            + "}"
                    ).getBytes(StandardCharsets.UTF_8)
                ),
                output
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to handle request", e);
        }

        assert errorCode == -32602 : "Request with invalid parameters must produce the Invalid params (-32602) error";
        String response = new String(output.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(response.contains("-32602"));
        assertTrue(response.contains("error"));
        assertTrue(response.contains("theUserName"));
        assertTrue(response.contains("thePassword"));
    }

    @Test
    public void callMethodWithNoParametersAndAnnotations() throws Exception {
        EasyMock.expect(mockService.testMethod1(param1)).andReturn(param1);
        EasyMock.replay(mockService);
        jsonRpcServer.handleRequest(
            messageWithListParamsStream(1, "testMethod1", param1),
            byteArrayOutputStream
        );
        assertEquals(param1, result().textValue());
    }

    @Test
    public void callMethodWithValidatedParameterUsingArrayParams() throws Exception {
        EasyMock.expect(mockService.testMethod2(EasyMock.anyObject())).andReturn(param1);
        EasyMock.replay(mockService);

        jsonRpcServer.handleRequest(
            messageWithListParamsStream(1, "testMethod2", (Object) null),
            byteArrayOutputStream
        );

        assertThatErrorContainsOneValidationError(error(byteArrayOutputStream))
            .assertParamIndexEquals(0)
            .assertDetailContainsTokens("null")
            .assertJsonPointerEquals("/0");
    }

    @Test
    public void callMethodWithValidatedCompositeParameterUsingObjectParams() throws Exception {
        EasyMock.expect(
            mockService.testMethod3(
                EasyMock.anyString(),
                EasyMock.anyObject()
            )
        ).andReturn(param1);
        EasyMock.replay(mockService);

        TestObject testObject = new TestObject();
        testObject.setNotNullField(null);

        jsonRpcServer.handleRequest(
            messageWithMapParamsStream("testMethod3",
                "stringParam", "stringValue",
                "testObject", testObject
            ),
            byteArrayOutputStream
        );

        assertThatErrorContainsOneValidationError(error(byteArrayOutputStream))
            .assertParamNameEquals("testObject")
            .assertDetailContainsTokens("null")
            .assertJsonPointerEquals("/testObject/notNullField");
    }

    @Test
    public void callMethodWithValidatedCompositeParameterUsingArrayParams() throws Exception {
        EasyMock
            .expect(
                mockService.testMethod3(
                    EasyMock.eq(param1),
                    EasyMock.anyObject()
                )
            )
            .andReturn(param1);
        EasyMock.replay(mockService);

        TestObject testObject = new TestObject();
        testObject.setNotNullField(null);

        jsonRpcServer.handleRequest(
            messageWithListParamsStream(1, "testMethod3", param1, testObject),
            byteArrayOutputStream
        );

        assertThatErrorContainsOneValidationError(error(byteArrayOutputStream))
            .assertParamIndexEquals(1)
            .assertDetailContainsTokens("null")
            .assertJsonPointerEquals("/1/notNullField");
    }


    @Test
    public void callMethodWithNestedInvalidParameterUsingObjectParams() throws Exception {
        EasyMock.expect(
            mockService.testMethod4(
                EasyMock.anyObject(),
                EasyMock.anyObject()
            )
        ).andReturn(param1);
        EasyMock.replay(mockService);

        TestObject invalidTestObject = new TestObject();
        invalidTestObject.setIntValue(Integer.MAX_VALUE);

        TestObjectHolder testObjectHolder = new TestObjectHolder();
        testObjectHolder.getObjectMap().put(
            "invalidTestObj",
            invalidTestObject
        );

        jsonRpcServer.handleRequest(
            messageWithMapParamsStream("testMethod4",
                "testObject", new TestObject(),
                "testObjectHolder", testObjectHolder
            ),
            byteArrayOutputStream
        );

        assertThatErrorContainsOneValidationError(error(byteArrayOutputStream))
            .assertParamNameEquals("testObjectHolder")
            .assertDetailContainsTokens("less", "equal", "10")
            .assertJsonPointerEquals(
                "/testObjectHolder/objectMap/invalidTestObj/intValue"
            );
    }

    @Test
    public void callMethodWithNestedInvalidParameterUsingArrayParams() throws Exception {
        EasyMock.expect(
            mockService.testMethod4(
                EasyMock.anyObject(),
                EasyMock.anyObject()
            )
        ).andReturn(param1);
        EasyMock.replay(mockService);

        TestObject invalidTestObject = new TestObject();
        invalidTestObject.setShortString("tooLongStringValue");

        TestObjectHolder testObjectHolder = new TestObjectHolder();
        testObjectHolder.getObjectList().add(invalidTestObject);
        testObjectHolder.getObjectList().add(new TestObject());

        TestObject[] objects = new TestObject[3];
        objects[0] = new TestObject();
        objects[1] = new TestObject();
        TestObject testObject = new TestObject();
        testObject.setNotNullField(null);
        objects[2] = testObject;
        testObjectHolder.setObjectArray(objects);


        jsonRpcServer.handleRequest(
            messageWithListParamsStream(
                1,
                "testMethod4",
                new TestObject(),
                testObjectHolder
            ),
            byteArrayOutputStream
        );

        JsonNode error = error(byteArrayOutputStream);

        List<ValidationError> validationErrors =
            assertThatErrorContainsAListOfValidationErrors(error);

        assertEquals(2, validationErrors.size());

        for (ValidationError validationError : validationErrors) {
            validationError.assertParamIndexEquals(1);

            String jsonPointer = validationError.getJsonPointer();
            assertNotNull(jsonPointer);

            // the exact order of validation errors is not specified
            if (jsonPointer.contains("objectList")) {
                validationError
                    .assertDetailContainsTokens("size", "between", "1", "16")
                    .assertJsonPointerEquals("/1/objectList/1/shortString");
            } else if (jsonPointer.contains("objectArray")) {
                validationError
                    .assertDetailContainsTokens("null")
                    .assertJsonPointerEquals("/1/objectArray/2/notNullField");
            } else {
                fail("unexpected validation error occured: " + jsonPointer);
            }
        }
    }

    @Test
    public void callMethodWithInvalidCollectionSizes() throws Exception {
        EasyMock.expect(
            mockService.testMethod4(
                EasyMock.anyObject(),
                EasyMock.anyObject()
            )
        ).andReturn(param1);
        EasyMock.replay(mockService);

        TestObjectHolder testObjectHolder = new TestObjectHolder();

        TestObject[] objects = new TestObject[4];
        for (int i = 0; i < 4; i++) {
            testObjectHolder.getObjectList().add(new TestObject());
            testObjectHolder.getObjectMap().put("testObject" + i, new TestObject());
            objects[i] = new TestObject();
        }
        testObjectHolder.setObjectArray(objects);


        jsonRpcServer.handleRequest(
            messageWithMapParamsStream("testMethod4",
                "testObject", new TestObject(),
                "testObjectHolder", testObjectHolder
            ),
            byteArrayOutputStream
        );

        List<ValidationError> validationErrors =
            assertThatErrorContainsAListOfValidationErrors(error(byteArrayOutputStream));

        assertEquals(3, validationErrors.size());

        for (ValidationError validationError : validationErrors) {
            validationError.assertParamNameEquals("testObjectHolder");


            String jsonPointer = validationError.getJsonPointer();
            assertNotNull(jsonPointer);

            validationError.assertDetailContainsTokens("size", "between", "1", "3");

            // the exact order of validation errors is not specified
            if (jsonPointer.contains("objectList")) {
                assertEquals(
                    "/testObjectHolder/objectList",
                    jsonPointer
                );
            } else if (jsonPointer.contains("objectMap")) {
                assertEquals(
                    "/testObjectHolder/objectMap",
                    jsonPointer
                );
            } else if (jsonPointer.contains("objectArray")) {
                assertEquals(
                    "/testObjectHolder/objectArray",
                    jsonPointer
                );
            } else {
                fail("unexpected validation error occured: " + jsonPointer);
            }
        }
    }

    @Test
    public void callMethodWithInvalidMapKey() throws Exception {
        EasyMock.expect(
            mockService.testMethod4(
                EasyMock.anyObject(),
                EasyMock.anyObject()
            )
        ).andReturn(param1);
        EasyMock.replay(mockService);

        TestObjectHolder testObjectHolder = new TestObjectHolder();
        testObjectHolder.getObjectMap().put("veryLongKeyString", new TestObject());

        jsonRpcServer.handleRequest(
            messageWithListParamsStream(
                1,
                "testMethod4",
                new TestObject(),
                testObjectHolder
            ),
            byteArrayOutputStream
        );

        assertThatErrorContainsOneValidationError(error(byteArrayOutputStream))
            .assertParamIndexEquals(1)
            .assertDetailContainsTokens("size", "between", "1", "16")
            .assertJsonPointerEquals("/1/objectMap/veryLongKeyString");
    }

    @Test
    public void callMethodWithInvalidIntegerInTheArray() throws Exception {
        EasyMock.expect(
            mockService.testMethod4(
                EasyMock.anyObject(),
                EasyMock.anyObject()
            )
        ).andReturn(param1);
        EasyMock.replay(mockService);

        TestObjectHolder testObjectHolder = new TestObjectHolder();
        testObjectHolder.getIntList().add(Integer.MAX_VALUE);
        testObjectHolder.getIntList().add(2);

        jsonRpcServer.handleRequest(
            messageWithMapParamsStream("testMethod4",
                "testObject", new TestObject(),
                "testObjectHolder", testObjectHolder
            ),
            byteArrayOutputStream
        );

        assertThatErrorContainsOneValidationError(error(byteArrayOutputStream))
            .assertParamNameEquals("testObjectHolder")
            .assertDetailContainsTokens("less", "equal", "10")
            .assertJsonPointerEquals("/testObjectHolder/intList/1");
    }

    @Test
    public void callMethodWithParameterWhichHasOptionalFields() throws Exception {
        // java.util.Optional fields require a separate Jackson module.
        Jdk8Module module = new Jdk8Module();
        module.configureReadAbsentAsNull(true);
        objectMapper.registerModule(module);

        EasyMock.expect(
            mockService.testMethod5(
                EasyMock.anyObject()
            )
        ).andReturn(param1);
        EasyMock.replay(mockService);

        OptionalListHolder optionalListHolder = new OptionalListHolder();

        List<Optional<OptionalFieldHolder>> optList = optionalListHolder.getOptList().get();

        OptionalFieldHolder optionalFieldHolder = new OptionalFieldHolder();
        optionalFieldHolder.setOptString(Optional.of("veryLongStringForOptional"));
        optList.add(Optional.of(optionalFieldHolder));

        HashMap<String, Object> request = messageOfStream(
            1,
            "testMethod5",
            new Object[] { optionalListHolder }
        );
        ByteArrayInputStream inputStream = new ByteArrayInputStream(
            objectMapper.writeValueAsBytes(request)
        );

        jsonRpcServer.handleRequest(
            inputStream,
            byteArrayOutputStream
        );

        assertThatErrorContainsOneValidationError(error(byteArrayOutputStream))
            .assertParamIndexEquals(0)
            .assertDetailContainsTokens("size", "between", "1", "16")
            .assertJsonPointerEquals("/0/optList/1/optString");
    }

    @Test
    public void callMethodWithSingleVarargsParamUsingObjectParams() throws Exception {
        EasyMock.expect(mockService.testMethod6(EasyMock.anyObject())).andReturn(intParam1);
        EasyMock.replay(mockService);

        List<TestObject> testObjects = new ArrayList<>();
        testObjects.add(new TestObject());
        testObjects.add(new TestObject());
        TestObject invalidTestObject = new TestObject();
        invalidTestObject.setIntValue(Integer.MAX_VALUE);
        testObjects.add(invalidTestObject);

        jsonRpcServer.handleRequest(
            messageWithMapParamsStream(
                "testMethod6",
                "testObjects",
                testObjects
            ),
            byteArrayOutputStream
        );

        assertThatErrorContainsOneValidationError(error(byteArrayOutputStream))
            .assertParamNameEquals("testObjects")
            .assertDetailContainsTokens("less", "equal", "10")
            .assertJsonPointerEquals("/testObjects/2/intValue");
    }

    @Test
    public void callMethodWithSingleVarargsParamUsingArrayParams() throws Exception {
        EasyMock.expect(mockService.testMethod6(EasyMock.anyObject())).andReturn(intParam1);
        EasyMock.replay(mockService);

        TestObject invalidTestObject = new TestObject();
        invalidTestObject.setIntValue(Integer.MAX_VALUE);

        TestObject[] testObjects = new TestObject[3];
        testObjects[0] = new TestObject();
        testObjects[1] = invalidTestObject;
        testObjects[2] = new TestObject();

        jsonRpcServer.handleRequest(
            messageWithListParamsStream(
                1,
                "testMethod6",
                (Object[]) testObjects
            ),
            byteArrayOutputStream
        );

        assertThatErrorContainsOneValidationError(error(byteArrayOutputStream))
            .assertParamIndexEquals(1)
            .assertDetailContainsTokens("less", "equal", "10")
            .assertJsonPointerEquals("/1/intValue");
    }

    @Test
    public void callMethodWithSingleVarargsParamUsingArrayOfArrayParams() throws Exception {
        EasyMock.expect(
            mockService.testMethod7(
                EasyMock.anyObject(),
                EasyMock.anyObject(),
                EasyMock.anyObject()
            )
        ).andReturn(intParam1);
        EasyMock.replay(mockService);

        TestObject invalidTestObject = new TestObject();
        invalidTestObject.setIntValue(Integer.MAX_VALUE);

        TestObject[][] testObjects = new TestObject[3][3];

        testObjects[0] = new TestObject[]{
            new TestObject(),
            new TestObject(),
            new TestObject()
        };
        testObjects[1] = new TestObject[]{
            new TestObject(),
            invalidTestObject,
            new TestObject(),
        };
        testObjects[2] = new TestObject[]{
            new TestObject(),
            new TestObject(),
            new TestObject()
        };

        jsonRpcServer.handleRequest(
            messageWithListParamsStream(
                1,
                "testMethod7",
                (Object[]) testObjects
            ),
            byteArrayOutputStream
        );

        // Validation of nested arrays does not work at the moment.
        // This test should break if a validation provider starts to support it.
        assertEquals(intParam1, result().intValue());
    }

    @Test
    public void callMethodWithSingleVarargsParamWithoutParameters() throws Exception {
        EasyMock.expect(mockService.testMethod6()).andReturn(intParam1);
        EasyMock.replay(mockService);

        jsonRpcServer.handleRequest(
            messageWithListParamsStream(
                1,
                "testMethod6"
            ),
            byteArrayOutputStream
        );

        assertThatErrorContainsOneValidationError(error(byteArrayOutputStream))
            .assertParamIndexEquals(0)
            .assertDetailContainsTokens("empty")
            .assertJsonPointerEquals("/0");
    }

    @Test
    public void callMethodWithListOfListsParams() throws Exception {
        EasyMock.expect(mockService.testMethod8(EasyMock.anyObject())).andReturn(intParam1);
        EasyMock.replay(mockService);

        TestObject invalidTestObject = new TestObject();
        invalidTestObject.setIntValue(Integer.MAX_VALUE);

        List<List<TestObject>> testObjects = new ArrayList<>(3);

        List<TestObject> testObjects1 = new ArrayList<>(3);
        testObjects1.add(new TestObject());
        testObjects1.add(new TestObject());
        testObjects1.add(new TestObject());
        testObjects.add(testObjects1);

        List<TestObject> testObjects2 = new ArrayList<>(3);
        testObjects2.add(new TestObject());
        testObjects2.add(invalidTestObject);
        testObjects2.add(new TestObject());
        testObjects.add(testObjects2);

        List<TestObject> testObjects3 = new ArrayList<>(3);
        testObjects3.add(new TestObject());
        testObjects3.add(new TestObject());
        testObjects3.add(new TestObject());
        testObjects.add(testObjects3);

        jsonRpcServer.handleRequest(
            messageWithListParamsStream(
                1,
                "testMethod8",
                testObjects
            ),
            byteArrayOutputStream
        );

        assertThatErrorContainsOneValidationError(error(byteArrayOutputStream))
            .assertParamIndexEquals(0)
            .assertDetailContainsTokens("less", "equal", "10")
            .assertJsonPointerEquals("/0/1/1/intValue");
    }

    @Test
    public void callMethodWithVarargsIntsUsingObjectParams() throws Exception {
        EasyMock.expect(
            mockService.testMethod9(
                EasyMock.anyInt(),
                EasyMock.anyString(),
                EasyMock.anyObject()
            )
        ).andReturn(param1);
        EasyMock.replay(mockService);

        jsonRpcServer.handleRequest(
            messageWithMapParamsStream(
                "testMethod9",
                "intParam", intParam1,
                "stringParam", param1,
                "testInts", new int[] {}
            ),
            byteArrayOutputStream
        );

        assertThatErrorContainsOneValidationError(error(byteArrayOutputStream))
            .assertParamNameEquals("testInts")
            .assertDetailContainsTokens("empty")
            .assertJsonPointerEquals("/testInts");
    }

    @Test
    public void callMethodWithVarargsAndRegularParamsInFrontUsingArraysParams() throws Exception {
        EasyMock.expect(
            mockService.testMethod10(
                EasyMock.anyObject(),
                EasyMock.anyObject(),
                EasyMock.anyObject()
            )
        ).andReturn(param1);
        EasyMock.replay(mockService);

        TestObject invalidTestObject = new TestObject();
        invalidTestObject.setIntValue(Integer.MAX_VALUE);

        jsonRpcServer.handleRequest(
            messageWithListParamsStream(
                1,
                "testMethod10",
                new TestObject(),
                new TestObject(),
                new TestObject[] {
                    // the remaining parameters still need to be passed in an array,
                    // it can work without it only if method has one varargs parameter
                    new TestObject(),
                    invalidTestObject,
                    new TestObject()
                }
            ),
            byteArrayOutputStream
        );

        assertThatErrorContainsOneValidationError(error(byteArrayOutputStream))
            .assertParamIndexEquals(2)
            .assertDetailContainsTokens("less", "equal", "10")
            .assertJsonPointerEquals("/2/1/intValue");
    }

    @Test
    public void callMethodWithSimpleResponseObjectValidation() throws Exception {
        TestObject invalidTestObject = new TestObject();
        invalidTestObject.setIntValue(Integer.MAX_VALUE);

        EasyMock.expect(mockService.testMethod11(param1)).andReturn(invalidTestObject);
        EasyMock.replay(mockService);

        jsonRpcServer.handleRequest(
            messageWithListParamsStream(
                1,
                "testMethod11",
                param1
            ),
            byteArrayOutputStream
        );

        JsonNode error = error(byteArrayOutputStream);
        assertNotNull(error);
        assertErrorCodeIsInternalError(error);
    }

    @Test
    public void callMethodWithComplexResponseObjectValidation() throws Exception {
        TestObject invalidTestObject = new TestObject();
        invalidTestObject.setNotNullField(null);

        TestObjectHolder testObjectHolder = new TestObjectHolder();
        testObjectHolder.getObjectMap().put(
            "invalidTestObj",
            invalidTestObject
        );

        EasyMock.expect(mockService.testMethod12(param1)).andReturn(testObjectHolder);
        EasyMock.replay(mockService);

        jsonRpcServer.handleRequest(
            messageWithListParamsStream(
                1,
                "testMethod12",
                param1
            ),
            byteArrayOutputStream
        );

        JsonNode error = error(byteArrayOutputStream);
        assertNotNull(error);
        assertErrorCodeIsInternalError(error);

        assertEquals(1, testBeanValidationJsonRpcInterceptor.constraintViolations.size());
        ConstraintViolation<?> constraintViolation =
            testBeanValidationJsonRpcInterceptor.constraintViolations.iterator().next();
        assertEquals(
            testObjectHolder,
            constraintViolation.getExecutableReturnValue()
        );
        assertEquals(
            "testMethod12.<return value>.objectMap[invalidTestObj].notNullField",
            constraintViolation.getPropertyPath().toString()
        );
        assertTrue(constraintViolation.getMessage().contains("null"));
    }

    @Test
    public void callMethodWithPrimitiveVarargsAndResponseObjectValidation() throws Exception {
        TestObject invalidTestObject = new TestObject();
        invalidTestObject.setShortString("tooLongStringValue1TooLongStringValue2");

        EasyMock.expect(mockService.testMethod13(intParam1, intParam2)).andReturn(invalidTestObject);
        EasyMock.replay(mockService);

        jsonRpcServer.handleRequest(
            messageWithListParamsStream(
                1,
                "testMethod13",
                intParam1,
                intParam2
            ),
            byteArrayOutputStream
        );

        JsonNode error = error(byteArrayOutputStream);
        assertNotNull(error);
        assertErrorCodeIsInternalError(error);

        assertEquals(1, testBeanValidationJsonRpcInterceptor.constraintViolations.size());
        ConstraintViolation<?> constraintViolation =
            testBeanValidationJsonRpcInterceptor.constraintViolations.iterator().next();
        assertEquals(
            invalidTestObject,
            constraintViolation.getExecutableReturnValue()
        );
        assertEquals(
            "testMethod13.<return value>.shortString",
            constraintViolation.getPropertyPath().toString()
        );
        String message = constraintViolation.getMessage();
        assertTrue(
            message.contains("size")
                && message.contains("between")
                && message.contains("1")
                && message.contains("16")
        );
    }

    @Test
    public void callMethodWithNonPrimitiveVarargsAndResponseObjectValidation() throws Exception {
        TestObject invalidTestObject = new TestObject();
        invalidTestObject.setNotNullField(null);

        EasyMock.expect(mockService.testMethod14(param1, param2)).andReturn(invalidTestObject);
        EasyMock.replay(mockService);

        jsonRpcServer.handleRequest(
            messageWithListParamsStream(
                1,
                "testMethod14",
                param1,
                param2
            ),
            byteArrayOutputStream
        );

        JsonNode error = error(byteArrayOutputStream);
        assertNotNull(error);
        assertErrorCodeIsInternalError(error);

        assertEquals(1, testBeanValidationJsonRpcInterceptor.constraintViolations.size());
        ConstraintViolation<?> constraintViolation =
            testBeanValidationJsonRpcInterceptor.constraintViolations.iterator().next();
        assertEquals(
            invalidTestObject,
            constraintViolation.getExecutableReturnValue()
        );
        assertEquals(
            "testMethod14.<return value>.notNullField",
            constraintViolation.getPropertyPath().toString()
        );
        assertTrue(
            constraintViolation.getMessage().contains("null")
        );
    }

    private static void assertErrorCodeIsMethodParamsInvalid(JsonNode error) {
        assertEquals(
            ErrorResolver.JsonError.METHOD_PARAMS_INVALID.code,
            errorCode(error).intValue()
        );
        assertEquals(
            ErrorResolver.JsonError.METHOD_PARAMS_INVALID.message,
            errorMessage(error).textValue()
        );
    }

    private static void assertErrorCodeIsInternalError(JsonNode error) {
        assertEquals(
            ErrorResolver.JsonError.INTERNAL_ERROR.code,
            errorCode(error).intValue()
        );
        assertEquals(
            ErrorResolver.JsonError.INTERNAL_ERROR.message,
            errorMessage(error).textValue()
        );
    }

    private static List<ValidationError> assertThatErrorContainsAListOfValidationErrors(
        JsonNode error
    ) throws IOException {
        assertNotNull(error);
        assertErrorCodeIsMethodParamsInvalid(error);

        JsonNode errorData = errorData(error);
        assertNotNull(errorData);

        JsonNode errors = errorData.get("errors");

        ObjectMapper objectMapper = new ObjectMapper();
        List<ValidationError> validationErrors =
            objectMapper.readerForListOf(ValidationError.class).readValue(errors);

        assertNotNull(validationErrors);
        assertFalse(validationErrors.isEmpty());

        return validationErrors;
    }

    private static ValidationError assertThatErrorContainsOneValidationError(
        JsonNode error
    ) throws IOException {
        List<ValidationError> validationErrors =
            assertThatErrorContainsAListOfValidationErrors(error);

        assertEquals(1, validationErrors.size());

        return validationErrors.get(0);
    }

    private JsonNode result() throws IOException {
        return decodeAnswer(byteArrayOutputStream).get(RESULT);
    }

    protected interface ServiceInterfaceWithBeanValidationAnnotations {
        String testMethod1(String stringParam);

        String testMethod2(@Valid @NotNull @Size(max = 10) String stringParam);

        String testMethod3(
            @JsonRpcParam("stringParam") String stringParam,
            @JsonRpcParam("testObject") @Valid TestObject testObject
        );

        String testMethod4(
            @JsonRpcParam("testObject") @Valid TestObject testObject,
            @JsonRpcParam("testObjectHolder") @Valid TestObjectHolder testObjectHolder
        );

        String testMethod5(
            @Valid OptionalListHolder optList
        );

        Integer testMethod6(
            @JsonRpcParam("testObjects")
            @Valid
            @NotEmpty
            TestObject... testObjects
        );

        // array of arrays does not work currently,
        // it may be better to use collections instead
        Integer testMethod7(
            @JsonRpcParam("testObjects")
            @Valid
            @NotEmpty
            TestObject[]... testObjects
        );

        Integer testMethod8(
            @JsonRpcParam("testObjects")
            @Valid
            @NotEmpty
            List<@Valid List<@Valid TestObject>> testObjects
        );

        String testMethod9(
            @JsonRpcParam("intParam") @Min(1) @Max(10) int intParam,
            @JsonRpcParam("stringParam") String stringParam,
            @JsonRpcParam("testInts")
            @Valid
            @NotEmpty
            int... testInts
        );

        String testMethod10(
            @Valid TestObject firstTestObject,
            @Valid TestObject secondTestObject,
            @Valid TestObject... otherTestObjects
        );

        @Valid TestObject testMethod11(String param);

        @Valid TestObjectHolder testMethod12(String param);

        @Valid TestObject testMethod13(int... intParams);

        @Valid TestObject testMethod14(String... strings);
    }

    private static final class ValidationError {
        private Integer paramIndex;
        private String paramName;
        private String detail;
        private String jsonPointer;

        public Integer getParamIndex() {
            return paramIndex;
        }

        public void setParamIndex(Integer paramIndex) {
            this.paramIndex = paramIndex;
        }

        public String getParamName() {
            return paramName;
        }

        public void setParamName(String paramName) {
            this.paramName = paramName;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }

        public String getJsonPointer() {
            return jsonPointer;
        }

        public void setJsonPointer(String jsonPointer) {
            this.jsonPointer = jsonPointer;
        }

        public ValidationError assertParamIndexEquals(int expectedIndex) {
            assertEquals(Integer.valueOf(expectedIndex), getParamIndex());
            assertNull(getParamName());
            return this;
        }

        public ValidationError assertParamNameEquals(String expectedParamName) {
            assertEquals(expectedParamName, getParamName());
            assertNull(getParamIndex());
            return this;
        }

        public ValidationError assertDetailContainsTokens(String... tokens) {
            String actualDetail = getDetail();
            assertNotNull(actualDetail);
            String actualDetailLowerCase = actualDetail.toLowerCase(Locale.ROOT);
            for (String token : tokens) {
                assertTrue(
                    actualDetailLowerCase.contains(token)
                );
            }
            return this;
        }

        public void assertJsonPointerEquals(String expectedJsonPointer) {
            assertEquals(expectedJsonPointer, getJsonPointer());
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ValidationError)) return false;
            ValidationError that = (ValidationError) o;
            return Objects.equals(paramIndex, that.paramIndex)
                && Objects.equals(paramName, that.paramName)
                && Objects.equals(detail, that.detail)
                && Objects.equals(jsonPointer, that.jsonPointer);
        }

        @Override
        public int hashCode() {
            return Objects.hash(paramIndex, paramName, detail, jsonPointer);
        }
    }

    protected static final class TestObject {
        @NotNull
        private String notNullField;

        @Size(min = 1, max = 16)
        private String shortString;

        @Min(0)
        @Max(10)
        private int intValue;

        public TestObject() {
            this.notNullField = "notNullStringValue";
            this.shortString = "shortStringValue";
            this.intValue = 1;
        }

        public String getNotNullField() {
            return notNullField;
        }

        public void setNotNullField(String notNullField) {
            this.notNullField = notNullField;
        }

        public String getShortString() {
            return shortString;
        }

        public void setShortString(String shortString) {
            this.shortString = shortString;
        }

        public int getIntValue() {
            return intValue;
        }

        public void setIntValue(int intValue) {
            this.intValue = intValue;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof TestObject)) return false;
            TestObject that = (TestObject) o;
            return intValue == that.intValue
                && Objects.equals(notNullField, that.notNullField)
                && Objects.equals(shortString, that.shortString);
        }

        @Override
        public int hashCode() {
            return Objects.hash(notNullField, shortString, intValue);
        }
    }

    protected static final class TestObjectHolder {
        TestObject testObject;

        @NotEmpty
        @Size(min = 1, max = 3)
        List<@Valid TestObject> objectList;

        @NotEmpty
        @Size(min = 1, max = 3)
        Map<@Valid @Size(min = 1, max = 16) String, @Valid TestObject> objectMap;

        @Valid
        @NotEmpty
        @Size(min = 1, max = 3)
        TestObject[] objectArray;

        @NotEmpty
        @Size(min = 1, max = 3)
        List<@Valid @Min(0) @Max(10) Integer> intList;

        public TestObjectHolder() {
            this.testObject = new TestObject();

            this.objectList = new ArrayList<>();
            this.objectList.add(new TestObject());

            this.objectMap = new LinkedHashMap<>();
            this.objectMap.put("testObject", new TestObject());

            this.objectArray = new TestObject[]{new TestObject()};

            this.intList = new ArrayList<>();
            this.intList.add(0);
        }

        public TestObject getTestObject() {
            return testObject;
        }

        public void setTestObject(TestObject testObject) {
            this.testObject = testObject;
        }

        public List<TestObject> getObjectList() {
            return objectList;
        }

        public void setObjectList(List<TestObject> objectList) {
            this.objectList = objectList;
        }

        public Map<String, TestObject> getObjectMap() {
            return objectMap;
        }

        public void setObjectMap(Map<String, TestObject> objectMap) {
            this.objectMap = objectMap;
        }

        public TestObject[] getObjectArray() {
            return objectArray;
        }

        public void setObjectArray(TestObject[] objectArray) {
            this.objectArray = objectArray;
        }

        public List<Integer> getIntList() {
            return intList;
        }

        public void setIntList(List<Integer> intList) {
            this.intList = intList;
        }
    }

    protected static final class OptionalFieldHolder {

        // java.util.Optional types in class fields are bad and should not be used,
        // but there may be existing code, which already uses those.
        @SuppressWarnings("all")
        Optional<@Valid @Size(min = 1, max = 16) String> optString;

        public OptionalFieldHolder() {
            this.optString = Optional.of("shortStringValue");
        }

        public Optional<String> getOptString() {
            return optString;
        }

        @SuppressWarnings("all")
        public void setOptString(Optional<String> optString) {
            this.optString = optString;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof OptionalFieldHolder)) return false;
            OptionalFieldHolder that = (OptionalFieldHolder) o;
            return Objects.equals(optString, that.optString);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(optString);
        }
    }

    protected static final class OptionalListHolder {
        @SuppressWarnings("all")
        Optional<@Valid List<@Valid Optional<@Valid OptionalFieldHolder>>> optList;

        public OptionalListHolder() {
            this.optList = Optional.of(new ArrayList<>());
            this.optList.get().add(Optional.of(new OptionalFieldHolder()));
        }

        public Optional<List<Optional<OptionalFieldHolder>>> getOptList() {
            return optList;
        }

        @SuppressWarnings("all")
        public void setOptList(Optional<List<Optional<OptionalFieldHolder>>> optList) {
            this.optList = optList;
        }
    }

    private static class TestBeanValidationJsonRpcInterceptor
        extends BeanValidationJsonRpcInterceptor {

        private final Set<ConstraintViolation<?>> constraintViolations = new HashSet<>();

        @Override
        protected void handleResponseObjectConstraintViolations(
            Set<ConstraintViolation<Object>> constraintViolations
        ) {
            this.constraintViolations.addAll(constraintViolations);
            super.handleResponseObjectConstraintViolations(constraintViolations);
        }
    }
}

import * as cdk from 'aws-cdk-lib';
import {aws_logs, CustomResource, RemovalPolicy, SecretValue} from 'aws-cdk-lib';
import {Construct} from 'constructs';
import * as apigateway from 'aws-cdk-lib/aws-apigateway';
import {AuthorizationType} from 'aws-cdk-lib/aws-apigateway';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as opensearch from 'aws-cdk-lib/aws-opensearchservice';
import {EngineVersion} from 'aws-cdk-lib/aws-opensearchservice';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import {SubnetType} from 'aws-cdk-lib/aws-ec2';
import * as kda from 'aws-cdk-lib/aws-kinesisanalyticsv2'
import * as assets from 'aws-cdk-lib/aws-s3-assets'
import * as logs from 'aws-cdk-lib/aws-logs';
import * as kinesis from 'aws-cdk-lib/aws-kinesis';
import {Provider} from "aws-cdk-lib/custom-resources";
import {IAMClient, ListRolesCommand} from "@aws-sdk/client-iam";
import * as cognito from 'aws-cdk-lib/aws-cognito';
import {AdvancedSecurityMode} from 'aws-cdk-lib/aws-cognito';
import {UserPoolUser} from "./UserPoolUser";

export class RealTimeSocialMediaAnalyticsGenAi extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);


    const opensearchUsername = "FlinkGenAIBedrock";
    const opensearchPassword  = "FlinkGenAIBedrock2024!"
    const cognitoUsername = 'FlinkGenAIBedrock@example.com';
    const cognitoPassword = "FlinkGenAIBedrock2024!";

    //Jar Connectors
    const flinkApplicationJar = new assets.Asset(this, 'flinkApplicationJar', {
      path: ('./flink-bedrock/target/flink-bedrock-1.0-SNAPSHOT.jar'),
    });

    // Cognito User Pool with Email Sign-in Type.
    const userPool = new cognito.UserPool(this, 'userPool', {
      signInAliases: {
        email: true
      },
      advancedSecurityMode:AdvancedSecurityMode.ENFORCED,
      passwordPolicy:{
        minLength: 8,
        requireUppercase:true,
        requireDigits: true,
        requireSymbols:true
      },
      removalPolicy: RemovalPolicy.DESTROY,
    })

    const userPoolClient = new cognito.UserPoolClient(this, 'UserPoolClient', {
      userPool: userPool,
      authFlows: {
        adminUserPassword: true,
        custom: false,
        userPassword: false,
        userSrp: true,
      },
      generateSecret: true, // Don't need to generate client secret
    });

    new UserPoolUser(this, 'CognitoUser', {
      userPool,
      username: cognitoUsername,
      password: cognitoPassword,
      attributes: []
    });

    // Create the VPC where MFA and OpenSearch will reside
    const vpc = new ec2.Vpc(this, "StreamingVPC", {
      maxAzs: 2,
      vpcName: "StreamingVPC",
    });

    // Create security group for OpenSearch cluster
    const opensearchSecurityGroup = new ec2.SecurityGroup(
        this,
        "OpensearchSecurityGroup",
        {
          vpc: vpc,
          securityGroupName: "OpensearchSecurityGroup",
        }
    );

    // Create security group for OpenSearch cluster
    const lambdaSecurityGroup = new ec2.SecurityGroup(
        this,
        "LambdaSecurityGroup",
        {
          vpc: vpc,
          securityGroupName: "LambdaSecurityGroup",
        }
    );

    // Create security group for Flink application
    const flinkSecurityGroup = new ec2.SecurityGroup(
        this,
        "FlinkSecurityGroup",
        {
          vpc: vpc,
          allowAllOutbound: true,
          securityGroupName: "FlinkSecurityGroup",
        }
    );

    opensearchSecurityGroup.addIngressRule(flinkSecurityGroup, ec2.Port.tcp(443));
    opensearchSecurityGroup.addIngressRule(lambdaSecurityGroup,ec2.Port.tcp(443));

    const kinesisVpcEndpointSecurityGroup = new ec2.SecurityGroup(
        this,
        "KinesisVpcEndpointSecurityGroup",
        {
          vpc: vpc,
          allowAllOutbound: true,
          securityGroupName: "KinesisVpcEndpointSecurityGroup",
        }
    );

    const kinesisVpcEndpoint = new ec2.InterfaceVpcEndpoint(
        this,
        "KinesisInterfaceEndpoint", {
          vpc: vpc,
          service: ec2.InterfaceVpcEndpointAwsService.KINESIS_STREAMS,
          subnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
          securityGroups: [kinesisVpcEndpointSecurityGroup]
        }
    )

    kinesisVpcEndpoint.node.addDependency(vpc)

    const bedRockPolicy = new iam.PolicyDocument({
      statements: [
        new iam.PolicyStatement({
          effect: iam.Effect.ALLOW,
          resources: ["arn:aws:bedrock:"+this.region+"::foundation-model/anthropic.claude-3-haiku-20240307-v1:0","arn:aws:bedrock:"+this.region+"::foundation-model/amazon.titan-embed-text-v1"],
          actions: ["bedrock:GetFoundationModel","bedrock:InvokeModel"]
        })
      ],
    });

    // our KDA app needs to be able to log
    const vpcPolicy = new iam.PolicyDocument({
      statements: [
        new iam.PolicyStatement({
          resources: ["*"],
          actions: ["ec2:DescribeVpcs",
            "ec2:DescribeSubnets",
            "ec2:DescribeSecurityGroups",
            "ec2:DescribeDhcpOptions",
            "ec2:DescribeNetworkInterfaces",
            "ec2:CreateNetworkInterface",
            "ec2:CreateNetworkInterfacePermission",
            "ec2:DeleteNetworkInterface"
          ]
        }),
      ],
    });

    const LambdaBedRockRole = new iam.Role(this, 'Lambda BedRock Role', {
      assumedBy: new iam.ServicePrincipal('lambda.amazonaws.com'),
      roleName: this.stackName+"bedrock-role",
      description: 'BedRock Role',
      inlinePolicies: {
        BedRockRole: bedRockPolicy,
        vpcPolicy: vpcPolicy
      },
    });

    // Define a Lambda layers
    const langChainLayer = new lambda.LayerVersion(this, 'langchain_0_1_20', {
      compatibleRuntimes: [lambda.Runtime.PYTHON_3_10],
      code: lambda.Code.fromAsset('./lambda-layers/langchain_0_1_20'), // Replace with the path to your layer code
      compatibleArchitectures: [lambda.Architecture.ARM_64, lambda.Architecture.X86_64]
    });

    const langChainCommunityLayer = new lambda.LayerVersion(this, 'langchain-community_0_0_33', {
      compatibleRuntimes: [lambda.Runtime.PYTHON_3_10],
      code: lambda.Code.fromAsset('./lambda-layers/langchain-community_0_0_33'), // Replace with the path to your layer code
      compatibleArchitectures: [lambda.Architecture.ARM_64, lambda.Architecture.X86_64]
    });

    const iamClient = new IAMClient();

    // Service-linked role that Amazon OpenSearch Service will use
    (async () => {
      const response = await iamClient.send(
          new ListRolesCommand({
            PathPrefix: "/aws-service-role/opensearchservice.amazonaws.com/",
          })
      );

      // Only if the role for OpenSearch Service doesn"t exist, it will be created.
      if (response.Roles && response.Roles?.length == 0) {
        new iam.CfnServiceLinkedRole(this, "OpensearchServiceLinkedRole", {
          awsServiceName: "es.amazonaws.com",
        });
      }
    })();

    // OpenSearch domain
    const opensearchRAGDatabase = new opensearch.Domain(this, "Domain", {
      version: EngineVersion.OPENSEARCH_2_11,
      vpc: vpc,
      securityGroups: [opensearchSecurityGroup],
      useUnsignedBasicAuth: true,
      fineGrainedAccessControl: {
        masterUserName: opensearchUsername,
        masterUserPassword: SecretValue.unsafePlainText(opensearchPassword),
      },
      nodeToNodeEncryption: true,
      enforceHttps: true,
      domainName: "opensearch2",
      encryptionAtRest: {
        enabled: true,
      },
      capacity: {
        dataNodes: 2,
        masterNodes: 0,
        dataNodeInstanceType: 'r6g.large.search',
        multiAzWithStandbyEnabled: false
      },
      ebs: {
        volumeSize: 30,
        volumeType: ec2.EbsDeviceVolumeType.GP3,
        throughput: 125,
        iops: 3000
      },
      removalPolicy: RemovalPolicy.DESTROY,
      zoneAwareness: {
        enabled: true,
      },
      offPeakWindowEnabled: true
    });

    opensearchRAGDatabase.addAccessPolicies(
        new iam.PolicyStatement({
          principals: [new iam.AnyPrincipal()],
          actions: ["es:ESHttp*"],
          resources: [opensearchRAGDatabase.domainArn + "/*"],
        })
    );

    // Manually identify the service-linked role associated with OpenSearch
    const serviceLinkedRole = iam.Role.fromRoleArn(
        this,
        "ServiceLinkedRole",
        "arn:aws:iam::aws:role/service-role/opensearchservice.amazonaws.com/AWSServiceRoleForAmazonOpenSearchService"
    );

    opensearchRAGDatabase.node.addDependency(serviceLinkedRole)

    // our KDA app needs access to describe kinesisanalytics
    const kdaAccessPolicy = new iam.PolicyDocument({
      statements: [
        new iam.PolicyStatement({
          resources: ["arn:aws:kinesisanalytics:"+this.region+":"+this.account+":application/flink-twitter-application"],
          actions: ['kinesisanalyticsv2:DescribeApplication','kinesisAnalyticsv2:UpdateApplication']
        }),
      ],
    });

    const logGroup = new logs.LogGroup(this, 'MyLogGroup', {
      retention: logs.RetentionDays.ONE_MONTH,
      removalPolicy: RemovalPolicy.DESTROY,
      logGroupName: this.stackName.toLowerCase()+"-log-group"// Adjust retention as needed
    });

    const logStream = new logs.LogStream(this, 'MyLogStream', {
      logGroup,
    });

    const accessCWLogsPolicy = new iam.PolicyDocument({
      statements: [
        new iam.PolicyStatement({
          resources: ["arn:aws:logs:" + this.region + ":" + this.account + ":log-group:"+this.stackName.toLowerCase()+"-log-group",
            "arn:aws:logs:" + this.region + ":" + this.account + ":log-group:flink-genai-log-group:log-stream:" + logStream.logStreamName],
          actions: ['logs:PutLogEvents','logs:DescribeLogGroups','logs:DescribeLogStreams','cloudwatch:PutMetricData'],
        }),
      ],
    });

    const s3Policy = new iam.PolicyDocument({
      statements: [
        new iam.PolicyStatement({
          resources: ['arn:aws:s3:::'+flinkApplicationJar.s3BucketName+'/',
            'arn:aws:s3:::'+flinkApplicationJar.s3BucketName+'/'+flinkApplicationJar.s3ObjectKey],
          actions: ['s3:ListBucket','s3:GetBucketLocation','s3:GetObject','s3:GetObjectVersion']
        }),
      ],
    });

    // Create a Kinesis Data Stream
    const kinesisStream = new kinesis.Stream(this, 'MyKinesisStream', {
      streamName: "rag-stream",
      shardCount: 1, // You can adjust the number of shards based on your requirements,
    });

    // Create a Kinesis Data Stream
    const kinesisRulesStream = new kinesis.Stream(this, 'MyKinesisRulesStream', {
      streamName: "rules-stream",
      shardCount: 1, // You can adjust the number of shards based on your requirements
    });

    // our KDA app needs to be able to log
    const kinesisPolicy = new iam.PolicyDocument({
      statements: [
        new iam.PolicyStatement({
          resources: [kinesisStream.streamArn,kinesisRulesStream.streamArn],
          actions: ["kinesis:DescribeStream",
            "kinesis:GetShardIterator",
            "kinesis:GetRecords",
            "kinesis:PutRecord",
            "kinesis:PutRecords",
            "kinesis:ListShards"
          ]
        }),
      ],
    });


    const managedFlinkRole = new iam.Role(this, 'Managed Flink Role', {
      assumedBy: new iam.ServicePrincipal('kinesisanalytics.amazonaws.com'),
      description: 'Managed Flink BedRock Role',
      inlinePolicies: {
        BedRockRole: bedRockPolicy,
        KDAAccessPolicy: kdaAccessPolicy,
        AccessCWLogsPolicy: accessCWLogsPolicy,
        S3Policy: s3Policy,
        KinesisPolicy:kinesisPolicy,
        vpcPolicy:vpcPolicy
      },
    });

    const managedFlinkApplication = new kda.CfnApplication(this, 'Managed Flink Application', {
      applicationName: 'flink-twitter-application',
      runtimeEnvironment: 'FLINK-1_18',
      serviceExecutionRole: managedFlinkRole.roleArn,
      applicationConfiguration: {
        vpcConfigurations : [
          {
            subnetIds: vpc.selectSubnets({
              subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS,
            }).subnetIds,
            securityGroupIds: [flinkSecurityGroup.securityGroupId],
          },
        ]
      ,
        applicationCodeConfiguration: {
          codeContent: {
            s3ContentLocation: {
              bucketArn: 'arn:aws:s3:::'+flinkApplicationJar.s3BucketName,
              fileKey: flinkApplicationJar.s3ObjectKey
            }
          },
          codeContentType: "ZIPFILE"
        },
        applicationSnapshotConfiguration: {
          snapshotsEnabled: true
        },
        environmentProperties: {
          propertyGroups: [{
            propertyGroupId: 'FlinkApplicationProperties',
            propertyMap: {
              'os.domain': opensearchRAGDatabase.domainEndpoint,
              'os.user': opensearchUsername,
              'os.password':opensearchPassword,
              'os.index': 'twitter-rag-index',
              'os.custom.index': 'twitter-custom-rag-index',
              'kinesis.source.stream': kinesisStream.streamName,
              'kinesis.rules.stream': kinesisRulesStream.streamName,
              'region':this.region
            },
          }],
        },

        flinkApplicationConfiguration: {
          parallelismConfiguration: {
            parallelism: 1,
            configurationType: 'CUSTOM',
            parallelismPerKpu: 1,
            autoScalingEnabled: false

          },
          monitoringConfiguration: {
            configurationType: "CUSTOM",
            metricsLevel: "APPLICATION",
            logLevel: "INFO"
          },
        }

      }
    });


    const cfnApplicationCloudWatchLoggingOption= new kda.CfnApplicationCloudWatchLoggingOption(this,"managedFlinkLogs", {
      applicationName: "flink-twitter-application",
      cloudWatchLoggingOption: {
        logStreamArn: "arn:aws:logs:" + this.region + ":" + this.account + ":log-group:flink-genai-log-group:log-stream:" + logStream.logStreamName,
      },
    });

    managedFlinkApplication.node.addDependency(managedFlinkRole)
    cfnApplicationCloudWatchLoggingOption.node.addDependency(managedFlinkApplication)

    managedFlinkApplication.node.addDependency(opensearchRAGDatabase)


    const twitterRagFunction = new lambda.Function(this, 'Twitter RAG Function', {
      runtime: lambda.Runtime.PYTHON_3_10,
      functionName: "langchainLambda",
      handler: 'lambda_function.lambda_handler',
      vpc:vpc,
      securityGroups:[lambdaSecurityGroup],
      vpcSubnets: {subnetType: SubnetType.PRIVATE_WITH_EGRESS},
      role: LambdaBedRockRole,
      code: lambda.Code.fromAsset('./twitter-rag-function'),
      memorySize: 2048,
      timeout: cdk.Duration.seconds(300),
      environment: {
        aosDomain: "https://"+opensearchRAGDatabase.domainEndpoint,
        aosUser: opensearchUsername,
        aosPassword: opensearchPassword,
        aosIndex: "twitter-rag-index",
        aosCustomIndex:'twitter-custom-rag-index',
        region: this.region
      },
      initialPolicy: [
        new iam.PolicyStatement({
          actions: ["logs:CreateLogStream", "logs:PutLogEvents"],
          resources: ['arn:aws:logs:' + this.region + ":" + this.account + ':log-group:/aws/lambda/langchainLambda'],
        })
      ]
    });

    const opensearchIndexCreationFunction = new lambda.Function(this, 'OpenSearch Index Creation Function', {
      runtime: lambda.Runtime.PYTHON_3_12,
      functionName: "opensearch-index-function",
      handler: 'lambda_function.lambda_handler',
      vpc:vpc,
      securityGroups:[lambdaSecurityGroup],
      vpcSubnets: {subnetType: SubnetType.PRIVATE_WITH_EGRESS},
      role: LambdaBedRockRole,
      code: lambda.Code.fromAsset('./index-creation-function'),
      memorySize: 2048,
      timeout: cdk.Duration.seconds(300),
      environment: {
        aosDomain: "https://"+opensearchRAGDatabase.domainEndpoint,
        aosUser: opensearchUsername,
        aosPassword: opensearchPassword,
      },
      initialPolicy: [
        new iam.PolicyStatement({
          actions: ["logs:CreateLogStream", "logs:PutLogEvents"],
          resources: ['arn:aws:logs:' + this.region + ":" + this.account + ':log-group:/aws/lambda/langchainLambda'],
        })
      ]
    });


    const startLambdaIndexFunctionProvider = new Provider(this, "startLambdaIndexFunctionProvider", {
      onEventHandler: opensearchIndexCreationFunction,
      logRetention: aws_logs.RetentionDays.ONE_WEEK
    })

    const startLambdaIndexFunctionResource = new CustomResource(this, "startLambdaIndexFunctionResource", {
      serviceToken: startLambdaIndexFunctionProvider.serviceToken,
    })

    startLambdaIndexFunctionResource.node.addDependency(opensearchRAGDatabase)

    const startFlinkApplicationHandler = new lambda.Function(this, "startFlinkApplicationHandler", {
      runtime: lambda.Runtime.PYTHON_3_12,
      code: lambda.Code.fromAsset("./startFlinkApplication"),
      handler: "index.on_event",
      timeout: cdk.Duration.minutes(14),
      memorySize: 512
    })

    const startFlinkApplicationProvider = new Provider(this, "startFlinkApplicationProvider", {
      onEventHandler: startFlinkApplicationHandler,
      logRetention: aws_logs.RetentionDays.ONE_WEEK
    })

    startFlinkApplicationHandler.addToRolePolicy(new iam.PolicyStatement({
      actions: [
        "kinesisanalytics:DescribeApplication",
        "kinesisanalytics:StartApplication",
        "kinesisanalytics:StopApplication",

      ],
      resources: [`arn:aws:kinesisanalytics:${this.region}:${this.account}:application/flink-twitter-application`]
    }))

    const startFlinkApplicationResource = new CustomResource(this, "startFlinkApplicationResource", {
      serviceToken: startFlinkApplicationProvider.serviceToken,
      properties: {
        AppName: 'flink-twitter-application',
      }
    })

    startFlinkApplicationResource.node.addDependency(managedFlinkApplication);

    // Add the layer using the layer ARN
    const boto3LayerARN = 'arn:aws:lambda:' + this.region + ':770693421928:layer:Klayers-p310-boto3:13';
    const opensearchLayerARN = 'arn:aws:lambda:' + this.region + ':770693421928:layer:Klayers-p38-opensearch-py:17';

    twitterRagFunction.addLayers(langChainLayer, langChainCommunityLayer, lambda.LayerVersion.fromLayerVersionArn(this, 'Boto3Layer', boto3LayerARN), lambda.LayerVersion.fromLayerVersionArn(this, 'OpenSearchLayer', opensearchLayerARN));


    const cognitoAuthorizerBedrock = new apigateway.CognitoUserPoolsAuthorizer(this, 'cognitoAuthorizerBedrock', {
      cognitoUserPools: [userPool],
      authorizerName: 'cognitoAuthorizerBedrock',
      identitySource: 'method.request.header.Authorization',
    });

    const cognitoAuthorizerKinesisRules = new apigateway.CognitoUserPoolsAuthorizer(this, 'cognitoAuthorizerKinesisRules', {
      cognitoUserPools: [userPool],
      authorizerName: 'cognitoAuthorizerKinesisRules',
      identitySource: 'method.request.header.Authorization',
    });

    const cognitoAuthorizerKinesisProxy = new apigateway.CognitoUserPoolsAuthorizer(this, 'cognitoAuthorizerKinesisProxy', {
      cognitoUserPools: [userPool],
      authorizerName: 'cognitoAuthorizerKinesisProxy',
      identitySource: 'method.request.header.Authorization',
    });



    const api = new apigateway.LambdaRestApi(this, 'twitter-rag-api', {
      handler: twitterRagFunction,
      deploy: true,
      proxy: false,
      endpointConfiguration: {
        types: [apigateway.EndpointType.REGIONAL]
      }
    })


    const bedrockResource = api.root.addResource('bedrock');

    const requestValidator = new apigateway.RequestValidator(this, 'RequestValidadorBedrockAPI', {
      restApi: api,
      validateRequestBody: true,
      validateRequestParameters: true,
    });

    // Define JSON Schema for request validation
    const bedrockRequestModel = new apigateway.Model(this, 'RequestModel', {
      restApi: api,
      contentType: 'application/json',
      schema: {
        schema: apigateway.JsonSchemaVersion.DRAFT4,
        title: 'BedrockRequestSchema',
        type: apigateway.JsonSchemaType.OBJECT,
        properties: {
          message: { type: apigateway.JsonSchemaType.STRING },
          index: { type: apigateway.JsonSchemaType.STRING }
        },
        required: ['message', 'index']
      },
    });


    bedrockResource.addMethod('POST', undefined, {
      authorizer: cognitoAuthorizerBedrock,
      authorizationType: AuthorizationType.COGNITO,
      requestValidator,
      requestModels: { 'application/json': bedrockRequestModel },
      requestParameters: {
        'method.request.header.Authorization': true,
      },
    });

    // Create an IAM role for API Gateway to assume
    const apiGatewayRole = new iam.Role(this, 'ApiGatewayRole', {
      assumedBy: new iam.ServicePrincipal('apigateway.amazonaws.com'),
    });

    // Attach the necessary policies to the IAM role
    apiGatewayRole.addToPolicy(new iam.PolicyStatement({
      actions: ['kinesis:PutRecord'],
      resources: [kinesisStream.streamArn,kinesisRulesStream.streamArn],
    }));



    // Create the API Gateway and explicitly pass the IAM role
    const kinesisProxyApi = new apigateway.RestApi(this, 'KinesisProxyApi', {
      restApiName: 'Kinesis Proxy API',
      endpointConfiguration: {
        types: [apigateway.EndpointType.REGIONAL]},
      deploy: true,
    });


    // Create a resource for the API
    const kinesisProxyResource = kinesisProxyApi.root.addResource('kinesis');

    // Create request validator
    const requestValidatorKinesisProxy = new apigateway.RequestValidator(this, 'RequestValidatorKinesisProxy', {
      restApi: kinesisProxyApi,
      validateRequestBody: true,
      validateRequestParameters: true,
    });

// Define JSON Schema for Kinesis request validation
    const kinesisRequestModel = new apigateway.Model(this, 'KinesisRequestModel', {
      modelName:'kinesisProxyBodyModel',
      restApi: kinesisProxyApi,
      contentType: 'application/json',
      schema: {
        schema: apigateway.JsonSchemaVersion.DRAFT4,
        title: 'KinesisBodyRequestSchema',
        type: apigateway.JsonSchemaType.OBJECT,
        properties: {
          StreamName: { type: apigateway.JsonSchemaType.STRING },
          Data: { type: apigateway.JsonSchemaType.STRING }, // Specify format as 'base64'
          PartitionKey: { type: apigateway.JsonSchemaType.STRING }
        },
        required: ['StreamName', 'Data', 'PartitionKey']
      },
    });
    const putRecordMethodOptionsProxy = {
      authorizer: cognitoAuthorizerKinesisProxy,
      authorizationType: AuthorizationType.COGNITO,
      requestParameters: {
        ['method.request.header.Content-Type']: true,
      },
      requestValidator: requestValidatorKinesisProxy,
      requestModels: { 'application/json': kinesisRequestModel },
    };

    // Add a POST method to the resource with the Kinesis integration
    const putMethod = kinesisProxyResource.addMethod('POST', new apigateway.AwsIntegration({
          service: 'kinesis',
          region: this.region,
          integrationHttpMethod: 'POST',
          action: 'PutRecord',
          options: {
            credentialsRole: apiGatewayRole,
            passthroughBehavior: apigateway.PassthroughBehavior.WHEN_NO_TEMPLATES,
            requestParameters: {
              ['integration.request.header.Content-Type']: 'method.request.header.Content-Type',
            },
            integrationResponses: [
              {
                statusCode: '200',}]
          },
        }),
        putRecordMethodOptionsProxy);

    const methodResponse: apigateway.MethodResponse = {
      statusCode: '200'
    }


    putMethod.addMethodResponse(methodResponse)

    // Create the API Gateway and explicitly pass the IAM role
    const kinesisRulesProxyApi = new apigateway.RestApi(this, 'KinesisRulesProxyApi', {
      restApiName: 'Kinesis Rules Proxy API',
      endpointConfiguration: {
        types: [apigateway.EndpointType.REGIONAL]},
      deploy: true,
    });

    // Create a resource for the API
    const kinesisRulesProxyResource = kinesisRulesProxyApi.root.addResource('rules');

    // Create request validator
    const requestValidatorKinesisRules = new apigateway.RequestValidator(this, 'RequestValidatorKinesisRules', {
      restApi: kinesisRulesProxyApi,
      validateRequestBody: true,
      validateRequestParameters: true,
    });

    const kinesisRulesRequestModel = new apigateway.Model(this, 'KinesisRulesRequestModel', {
      restApi: kinesisRulesProxyApi,
      contentType: 'application/json',
      schema: {
        schema: apigateway.JsonSchemaVersion.DRAFT4,
        title: 'KinesisRulesRequestSchema',
        type: apigateway.JsonSchemaType.OBJECT,
        properties: {
          StreamName: { type: apigateway.JsonSchemaType.STRING },
          Data: { type: apigateway.JsonSchemaType.STRING }, // Specify format as 'base64'
          PartitionKey: { type: apigateway.JsonSchemaType.STRING }
        },
        required: ['StreamName', 'Data', 'PartitionKey']
      },
    });

    const putRecordMethodOptionsRules = {
      authorizer: cognitoAuthorizerKinesisRules,
      authorizationType: AuthorizationType.COGNITO,
      requestParameters: {
        ['method.request.header.Content-Type']: true,
      },
      requestValidator: requestValidatorKinesisRules,
      requestModels: { 'application/json': kinesisRulesRequestModel },
    };


    // Add a POST method to the resource with the Kinesis integration
    const putRulesMethod = kinesisRulesProxyResource.addMethod('POST', new apigateway.AwsIntegration({
          service: 'kinesis',
          region: this.region,
          integrationHttpMethod: 'POST',
          action: 'PutRecord',
          options: {
            credentialsRole: apiGatewayRole,
            passthroughBehavior: apigateway.PassthroughBehavior.WHEN_NO_TEMPLATES,
            requestParameters: {
              ['integration.request.header.Content-Type']: 'method.request.header.Content-Type',
            },
            integrationResponses: [
              {
                statusCode: '200',}]


          },
        }),
        putRecordMethodOptionsRules);


    putRulesMethod.addMethodResponse(methodResponse)

    // Output the API URL
    new cdk.CfnOutput(this, 'Streamlit Command', {
      value: "streamlit run Bedrock_Chatbot.py --theme.base \"dark\" --  --pool_id " + userPool.userPoolId + " --app_client_id " + userPoolClient.userPoolClientId + " --app_client_secret " + userPoolClient.userPoolClientSecret.unsafeUnwrap() + " --bedrock_api " + api.urlForPath("/bedrock") + " --flink_rules_api "+kinesisRulesProxyApi.urlForPath("/rules")+ " --kinesis_social_api " + kinesisProxyApi.urlForPath("/kinesis") ,
      description: 'Streamlit UI Command',
    });

  }

}

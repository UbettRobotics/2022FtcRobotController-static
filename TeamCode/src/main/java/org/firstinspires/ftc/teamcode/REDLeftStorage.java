package org.firstinspires.ftc.teamcode;
import static org.firstinspires.ftc.teamcode.Robot.basket;
import static org.firstinspires.ftc.teamcode.Robot.initAccessories;
import static org.firstinspires.ftc.teamcode.Robot.initMotors;
import static org.firstinspires.ftc.teamcode.Robot.slide;
import static org.firstinspires.ftc.teamcode.Robot.tablemotor;
import static org.firstinspires.ftc.teamcode.Robot.cap;
import static org.firstinspires.ftc.teamcode.Robot.capdefault;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import com.acmerobotics.roadrunner.trajectory.Trajectory;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.teamcode.drive.SampleMecanumDrive;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;

@Autonomous(name = "[-]RED Left:Storage", preselectTeleOp = "teleopV2")
public class REDLeftStorage extends LinearOpMode {

    OpenCvCamera webcam;
    enum RobotPath {
        BARRIER,
        GAP
    }
    RobotPath Path;

    final int START_X = -36;
    final int START_Y = -64;
    int level = 0;
    int height = 0;
    double basket_value = 0;
    double alignDistance = 0;

    @Override
    public void runOpMode() throws InterruptedException {
        ////////////init camera and motors ////////////////////////////////////////////////////////////////////
        telemetry.addLine("Not initialized");
        telemetry.update();

        SampleMecanumDrive drive = new SampleMecanumDrive(hardwareMap);
        initMotors(this);
        initAccessories(this);

        Pose2d startPose = new Pose2d(START_X, START_Y, Math.toRadians(-90)); //init starting position
        drive.setPoseEstimate(startPose);

        //////Start Camera Streaming//////

        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        webcam = OpenCvCameraFactory.getInstance().createWebcam(hardwareMap.get(WebcamName.class, "Webcam"), cameraMonitorViewId);

        ConeVisionPipeline pipeline = new ConeVisionPipeline(telemetry);
        webcam.setPipeline(pipeline);

        webcam.openCameraDeviceAsync(new OpenCvCamera.AsyncCameraOpenListener() {
            @Override
            public void onOpened() {
                webcam.startStreaming(320, 240, OpenCvCameraRotation.UPRIGHT);
            }

            @Override
            public void onError(int errorCode) {
                telemetry.addData("Error", errorCode);
                telemetry.addData("Please restart the program", 0);
                telemetry.update();
            }
        });



////////Program start////////////////////////////////////////////////////////////////////////

        waitForStart();
        ////Move on start/init
        basket.setPosition(Robot.basketdefault);
        cap.setPosition(capdefault);
        Path = RobotPath.BARRIER;
        ////

        telemetry.addData("location: ", pipeline.getSide());
        telemetry.update();
        switch(pipeline.getSide()) {
            case LEFT_SIDE:
                level = 1;
                height = 0;
                basket_value = 0.95;
                break;
            case MIDDLE_SIDE:
                level = 2;
                height = 1350;
                basket_value = 0.95;
                break;
            case RIGHT_SIDE:
                level = 3;
                height = 2050;
                basket_value = 0.93;

        }

        Trajectory inchForward = drive.trajectoryBuilder(startPose)
                .lineTo(new Vector2d(-36,-59))
                .build();
        Trajectory toCarousel = drive.trajectoryBuilder(inchForward.end())
                .lineToLinearHeading(new Pose2d(-12,-57,Math.toRadians(-159)))//to -180
                .build();
        Trajectory toTurn = drive.trajectoryBuilder(toCarousel.end().plus(new Pose2d(0,0,Math.toRadians(-21))))
                .lineTo(new Vector2d(-5, -16))
                .build();
        Trajectory lineToWall = drive.trajectoryBuilder(toTurn.end())
                .strafeRight(12)
                .build();
        drive.followTrajectory(inchForward);
        drive.followTrajectory(toCarousel);
        tablemotor.setPower(-0.5);
        sleep(2500);
        tablemotor.setPower(0);

        drive.followTrajectory(toTurn);
        drive.turn(Math.toRadians(-75));
        drive.setPoseEstimate(drive.getPoseEstimate().plus(new Pose2d(0, 0, Math.toRadians(270))));
        drive.followTrajectory(lineToWall);
        Trajectory toShippingHub2Short = drive.trajectoryBuilder(toTurn.end())//Bottom
                .strafeLeft(39.5)
                .build();
        Trajectory toShippingHub2Middle = drive.trajectoryBuilder(toTurn.end())//Middle
                .strafeLeft(40.5)
                .build();
        Trajectory toShippingHub2Long = drive.trajectoryBuilder(toTurn.end())//Top
                .strafeLeft(42)
                .build();



        if(level == 1) {
            drive.followTrajectory(toShippingHub2Short);
        } else if(level == 2) {
            drive.followTrajectory(toShippingHub2Middle);
        } else {
            drive.followTrajectory(toShippingHub2Long);
        }

        //Deliver


        slide.setTargetPosition(height);
        slide.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        slide.setPower(0.6);
        while(slide.isBusy()){}


        basket.setPosition(basket_value);
        sleep(4000);
        basket.setPosition(Robot.basketdefault);

        /*
        slide.setTargetPosition(0);
        slide.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        slide.setPower(0.6);
        */

        Trajectory align;
        Trajectory sprint;
        align = drive.trajectoryBuilder(toShippingHub2Long.end())
                .strafeRight(40)
                .build();
        Trajectory park = drive.trajectoryBuilder(align.end())
                .back(11)
                .build();
        drive.followTrajectory(align);
        drive.followTrajectory(park);


        slide.setTargetPosition(0);
        slide.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        slide.setPower(0.6);
        while(slide.isBusy()){}

        if (isStopRequested()) return;
        sleep(2000);

    }
}

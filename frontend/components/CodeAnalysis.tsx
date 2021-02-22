import React, {useEffect} from "react";
import { makeStyles, withStyles, Theme, createStyles } from '@material-ui/core/styles';
import { green, purple } from '@material-ui/core/colors';
import FormGroup from '@material-ui/core/FormGroup';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Checkbox, { CheckboxProps } from '@material-ui/core/Checkbox';
import Avatar from '@material-ui/core/Avatar';
import axios, {AxiosResponse} from "axios";
import {useRouter} from "next/router";

const GreenCheckbox = withStyles({
    root: {
        color: green[400],
        '&$checked': {
            color: green[600],
        },
    },
    checked: {},
})((props: CheckboxProps) => <Checkbox color="default" {...props} />);

const PurpleCheckbox = withStyles({
    root: {
        color: purple[400],
        '&$checked': {
            color: purple[600],
        },
    },
    checked: {},
})((props: CheckboxProps) => <Checkbox color="default" {...props} />);

// TODO: add graph tag where placeholder is

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        container1: {
            display: 'flex',
            width: '100%',
        },
        container2: {
            display: 'flex',
            justifyContent: 'flex-start',
            width: '70%',
        },
        outerContainer: {
            flexDirection: 'column',
            width: '100%',
        },
        textContainer1: {
            flexDirection: 'column',
            margin: '0px 0px 0px 20px',
            padding: '0px',
        },
        textContainer2: {
            flexDirection: 'column',
            width:'30%',
        },
        repoNameText: {
            fontSize: '2em',
            margin: '16px 0px',
        },
        smallTextColor: {
            color: 'grey',
            margin: '0px 0px',
        },
        mrScoreText: {
            fontSize:'1.2em',
            margin: '16px 0px',
            textAlign: 'right'
        },
        commitScoreText: {
            fontSize:'1.2em',
            margin: '0px 0px',
            textAlign: 'right'
        },
        container3: {
            display: 'flex',
            justifyContent: 'space-around',
            margin: '24px 0px',
        },
        graphContainer: {
            display: 'flex',
            justifyContent: 'space-around',
            flexDirection: 'column',
        },
        avatarSize: {
            width: theme.spacing(15),
            height: theme.spacing(15),
        },
    }),
);

const CodeAnalysis = () => {
    const classes = useStyles();
    const [checkboxState, setCheckboxState] = React.useState({
        checkedCommitForGraphA: true,
        checkedMergeRequestForGraphA: true,
        checkedCommitForGraphB: true,
        checkedMergeRequestForGraphB: true,
    });
    const [mergeRequestNumber, setMergeRequestNumber] = React.useState<number>();
    const [projectName, setProjectName] = React.useState<String>();
    const [commitNumber, setCommitNumber] = React.useState<number>();

    const router = useRouter();
    const { projectId } =  router.query;

    useEffect(() => {
        if (router.isReady) {
            axios
                .get(`${process.env.NEXT_PUBLIC_API_URL}/gitlab/projects/`+projectId)
                .then((resp: AxiosResponse) => {
                    setProjectName(resp.data.name_with_namespace);
                });
            axios
                .get(`${process.env.NEXT_PUBLIC_API_URL}/gitlab/projects/`+projectId+"/merge_requests?startDateTime=2020-09-01T14:00:00.000Z&endDateTime=2020-12-21T14:00:00.000Z")
                .then((resp: AxiosResponse) => {
                    setMergeRequestNumber(resp.data.length);
                });
            axios
                .get(`${process.env.NEXT_PUBLIC_API_URL}/gitlab/projects/`+projectId+"/commits?startDateTime=2020-08-03T14:00:00.000Z&endDateTime=2020-12-29T14:00:00.000Z")
                .then((resp: AxiosResponse) => {
                    setCommitNumber(resp.data.length);
                });
        }
    }, [projectId]);

    const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        setCheckboxState({ ...checkboxState, [event.target.name]: event.target.checked });
    };

    return (
    <>
        <div className={classes.outerContainer}>
            <div className={classes.container1}>
                <div className={classes.container2}>
                    <Avatar className={classes.avatarSize} variant='square'>R</Avatar>
                    <div className={classes.textContainer1}>
                        <h1 className={classes.repoNameText}>{projectName}</h1>
                        <p className={classes.smallTextColor}>- {commitNumber} Commits - {mergeRequestNumber} Merge Request -</p>
                    </div>
                </div>
                <div className={classes.textContainer2}>
                    <p className={classes.mrScoreText}>Merge Request Score: 300</p>
                    <p className={classes.commitScoreText}>Commit Score: 120</p>
                </div>
            </div>
            <div className={classes.container3}>
                <div className={classes.graphContainer}>
                    <p> (graph placeholder)            </p>
                </div>
                <FormGroup>
                    <FormControlLabel
                        control={<GreenCheckbox checked={checkboxState.checkedCommitForGraphA} onChange={handleChange} name="checkedCommitForGraphA"/>}
                        label="Commits"
                    />
                    <FormControlLabel
                        control={<PurpleCheckbox checked={checkboxState.checkedMergeRequestForGraphA} onChange={handleChange} name="checkedMergeRequestForGraphA"/>}
                        label="Merge Requests"
                    />
                </FormGroup>
            </div>
            <div className={classes.container3}>
                <div className={classes.graphContainer}>
                    <p> (graph placeholder)            </p>
                </div>
                <FormGroup>
                    <FormControlLabel
                        control={<GreenCheckbox checked={checkboxState.checkedCommitForGraphB} onChange={handleChange} name="checkedCommitForGraphB"/>}
                        label="Commits"
                    />
                    <FormControlLabel
                        control={<PurpleCheckbox checked={checkboxState.checkedMergeRequestForGraphB} onChange={handleChange} name="checkedMergeRequestForGraphB"/>}
                        label="Merge Requests"
                    />
                </FormGroup>
            </div>
        </div>
    </>
    );
};

export default CodeAnalysis;
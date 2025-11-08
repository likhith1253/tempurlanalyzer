import { useState } from 'react';
import { 
  Box, 
  Button, 
  Input, 
  VStack, 
  HStack, 
  Text, 
  Spinner, 
  Progress, 
  useToast,
  List,
  ListItem,
  ListIcon,
  Alert,
  AlertIcon,
  AlertTitle,
  AlertDescription,
} from '@chakra-ui/react';
import { CheckCircleIcon, WarningIcon, NotAllowedIcon } from '@chakra-ui/icons';
import { collection, addDoc, serverTimestamp } from 'firebase/firestore';
import { db } from '../firebase';

type AnalysisResult = {
  riskScore: number;
  decision: 'ALLOW' | 'WARN' | 'BLOCK';
  reason: string;
  evidence: string[];
  url: string;
};

export const AnalyzeURLBox = () => {
  const [url, setUrl] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [result, setResult] = useState<AnalysisResult | null>(null);
  const toast = useToast();

  const analyzeUrl = async () => {
    if (!url) {
      toast({
        title: 'Error',
        description: 'Please enter a URL to analyze',
        status: 'error',
        duration: 3000,
        isClosable: true,
      });
      return;
    }

    setIsLoading(true);
    try {
      const response = await fetch('/api/analyze/url', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ url }),
      });

      if (!response.ok) {
        throw new Error('Analysis failed');
      }

      const data = await response.json();
      setResult({
        ...data,
        url,
      });

      // Store analysis in Firestore
      try {
        await addDoc(collection(db, 'urlAnalyses'), {
          ...data,
          url,
          timestamp: serverTimestamp(),
        });
      } catch (firestoreError) {
        console.error('Error saving to Firestore:', firestoreError);
      }

    } catch (error) {
      toast({
        title: 'Analysis Error',
        description: 'Failed to analyze the URL. Please try again.',
        status: 'error',
        duration: 3000,
        isClosable: true,
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handleWhitelist = async () => {
    if (!result) return;
    
    try {
      await addDoc(collection(db, 'whitelist'), {
        url: result.url,
        reason: 'Manually whitelisted by user',
        timestamp: serverTimestamp(),
      });
      
      toast({
        title: 'URL Whitelisted',
        description: 'The URL has been added to your whitelist.',
        status: 'success',
        duration: 3000,
        isClosable: true,
      });
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to whitelist URL',
        status: 'error',
        duration: 3000,
        isClosable: true,
      });
    }
  };

  const handleReportFalsePositive = async () => {
    if (!result) return;
    
    try {
      await addDoc(collection(db, 'falsePositives'), {
        url: result.url,
        originalDecision: result.decision,
        timestamp: serverTimestamp(),
      });
      
      toast({
        title: 'Report Submitted',
        description: 'Thank you for your feedback. We will review this URL.',
        status: 'info',
        duration: 3000,
        isClosable: true,
      });
    } catch (error) {
      toast({
        title: 'Error',
        description: 'Failed to submit false positive report',
        status: 'error',
        duration: 3000,
        isClosable: true,
      });
    }
  };

  const getDecisionColor = () => {
    if (!result) return 'gray';
    switch (result.decision) {
      case 'ALLOW': return 'green';
      case 'WARN': return 'yellow';
      case 'BLOCK': return 'red';
      default: return 'gray';
    }
  };

  const getDecisionIcon = () => {
    if (!result) return null;
    switch (result.decision) {
      case 'ALLOW': return <CheckCircleIcon color="green.500" mr={2} />;
      case 'WARN': return <WarningIcon color="yellow.500" mr={2} />;
      case 'BLOCK': return <NotAllowedIcon color="red.500" mr={2} />;
      default: return null;
    }
  };

  return (
    <Box 
      p={6} 
      borderWidth="1px" 
      borderRadius="lg" 
      boxShadow="md"
      maxW="2xl"
      width="100%"
      bg="white"
    >
      <VStack spacing={4} align="stretch">
        <Text fontSize="xl" fontWeight="bold">URL Threat Analysis</Text>
        
        <HStack>
          <Input
            placeholder="Enter URL to analyze"
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            isDisabled={isLoading}
          />
          <Button 
            colorScheme="blue" 
            onClick={analyzeUrl}
            isLoading={isLoading}
            loadingText="Analyzing..."
          >
            Analyze
          </Button>
        </HStack>

        {isLoading && (
          <VStack spacing={4} py={4}>
            <Spinner size="lg" />
            <Text>Analyzing URL...</Text>
          </VStack>
        )}

        {result && (
          <VStack spacing={4} align="stretch" pt={4}>
            <Alert status={result.decision === 'ALLOW' ? 'success' : result.decision === 'WARN' ? 'warning' : 'error'} borderRadius="md">
              <AlertIcon />
              <Box>
                <AlertTitle>Decision: {result.decision}</AlertTitle>
                <AlertDescription>{result.reason}</AlertDescription>
              </Box>
            </Alert>

            <Box>
              <Text fontWeight="medium" mb={2}>Risk Score: {result.riskScore}%</Text>
              <Progress 
                value={result.riskScore} 
                size="sm" 
                colorScheme={result.riskScore > 70 ? 'red' : result.riskScore > 30 ? 'yellow' : 'green'}
                borderRadius="md"
              />
            </Box>

            <Box>
              <Text fontWeight="medium" mb={2}>Evidence:</Text>
              <List spacing={2}>
                {result.evidence.map((item, index) => (
                  <ListItem key={index} display="flex" alignItems="center">
                    <ListIcon as={getDecisionIcon} />
                    {item}
                  </ListItem>
                ))}
              </List>
            </Box>

            <HStack spacing={4} pt={4}>
              <Button 
                colorScheme="green" 
                variant="outline"
                onClick={handleWhitelist}
                leftIcon={<CheckCircleIcon />}
              >
                Add to Whitelist
              </Button>
              <Button 
                colorScheme="orange" 
                variant="outline"
                onClick={handleReportFalsePositive}
                leftIcon={<WarningIcon />}
              >
                Report False Positive
              </Button>
            </HStack>
          </VStack>
        )}
      </VStack>
    </Box>
  );
};

export default AnalyzeURLBox;

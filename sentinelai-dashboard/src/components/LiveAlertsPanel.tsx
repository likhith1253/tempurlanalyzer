import { useState, useEffect } from 'react';
import { 
  Box, 
  VStack, 
  HStack, 
  Text, 
  Select, 
  Input, 
  InputGroup, 
  InputLeftElement, 
  Button, 
  IconButton, 
  useDisclosure,
  Drawer,
  DrawerBody,
  DrawerHeader,
  DrawerOverlay,
  DrawerContent,
  DrawerCloseButton,
  Badge,
  useColorModeValue,
  Divider,
  Spinner,
  Tooltip,
  useToast,
} from '@chakra-ui/react';
import { SearchIcon, FilterIcon, X } from '@chakra-ui/icons';
import { collection, query, where, orderBy, limit, onSnapshot, Timestamp } from 'firebase/firestore';
import { db } from '../firebase';
import { formatDistanceToNow } from 'date-fns';

interface Alert {
  id: string;
  type: string;
  message: string;
  riskLevel: number;
  source: string;
  timestamp: Timestamp;
  decision: 'ALLOW' | 'WARN' | 'BLOCK';
  metadata?: Record<string, any>;
}

const ITEMS_PER_PAGE = 10;

export const LiveAlertsPanel = () => {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [filteredAlerts, setFilteredAlerts] = useState<Alert[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [filters, setFilters] = useState({
    decision: '',
    riskMin: '',
    riskMax: '',
    source: '',
    search: '',
  });
  
  const { isOpen, onOpen, onClose } = useDisclosure();
  const toast = useToast();
  const bgColor = useColorModeValue('white', 'gray.800');
  const borderColor = useColorModeValue('gray.200', 'gray.700');

  // Subscribe to Firestore alerts collection
  useEffect(() => {
    const q = query(
      collection(db, 'alerts'),
      orderBy('timestamp', 'desc'),
      limit(100) // Limit to 100 most recent alerts
    );

    const unsubscribe = onSnapshot(q, 
      (snapshot) => {
        const alertsData = snapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data(),
        })) as Alert[];
        
        setAlerts(alertsData);
        setIsLoading(false);
      },
      (error) => {
        console.error('Error fetching alerts:', error);
        toast({
          title: 'Error',
          description: 'Failed to load alerts',
          status: 'error',
          duration: 3000,
          isClosable: true,
        });
        setIsLoading(false);
      }
    );

    return () => unsubscribe();
  }, [toast]);

  // Apply filters
  useEffect(() => {
    let result = [...alerts];

    if (filters.decision) {
      result = result.filter(alert => alert.decision === filters.decision);
    }

    if (filters.riskMin) {
      const min = parseInt(filters.riskMin);
      result = result.filter(alert => alert.riskLevel >= min);
    }

    if (filters.riskMax) {
      const max = parseInt(filters.riskMax);
      result = result.filter(alert => alert.riskLevel <= max);
    }

    if (filters.source) {
      result = result.filter(alert => 
        alert.source.toLowerCase().includes(filters.source.toLowerCase())
      );
    }

    if (filters.search) {
      const searchTerm = filters.search.toLowerCase();
      result = result.filter(alert => 
        alert.message.toLowerCase().includes(searchTerm) ||
        (alert.metadata && JSON.stringify(alert.metadata).toLowerCase().includes(searchTerm))
      );
    }

    setFilteredAlerts(result);
    setPage(1); // Reset to first page when filters change
  }, [alerts, filters]);

  const handleFilterChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFilters(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const clearFilters = () => {
    setFilters({
      decision: '',
      riskMin: '',
      riskMax: '',
      source: '',
      search: '',
    });
  };

  const getDecisionColor = (decision: string) => {
    switch (decision) {
      case 'ALLOW': return 'green';
      case 'WARN': return 'yellow';
      case 'BLOCK': return 'red';
      default: return 'gray';
    }
  };

  const getRiskColor = (risk: number) => {
    if (risk >= 70) return 'red';
    if (risk >= 30) return 'orange';
    return 'green';
  };

  // Pagination
  const totalPages = Math.ceil(filteredAlerts.length / ITEMS_PER_PAGE);
  const paginatedAlerts = filteredAlerts.slice(
    (page - 1) * ITEMS_PER_PAGE,
    page * ITEMS_PER_PAGE
  );

  return (
    <Box 
      borderWidth="1px" 
      borderRadius="lg" 
      overflow="hidden"
      bg={bgColor}
      boxShadow="sm"
    >
      <Box p={4} borderBottomWidth="1px" borderColor={borderColor}>
        <HStack justify="space-between">
          <Text fontSize="lg" fontWeight="semibold">Live Security Alerts</Text>
          <HStack>
            <InputGroup maxW="300px">
              <InputLeftElement pointerEvents="none">
                <SearchIcon color="gray.400" />
              </InputLeftElement>
              <Input 
                placeholder="Search alerts..." 
                value={filters.search}
                onChange={(e) => handleFilterChange({
                  ...e,
                  target: { ...e.target, name: 'search' }
                })}
              />
            </InputGroup>
            <Button 
              leftIcon={<FilterIcon />} 
              variant="outline"
              onClick={onOpen}
            >
              Filters
            </Button>
          </HStack>
        </HStack>
      </Box>

      <Box overflowY="auto" maxH="600px">
        {isLoading ? (
          <Box p={8} textAlign="center">
            <Spinner size="xl" />
            <Text mt={4}>Loading alerts...</Text>
          </Box>
        ) : paginatedAlerts.length === 0 ? (
          <Box p={8} textAlign="center">
            <Text color="gray.500">No alerts found matching your criteria</Text>
          </Box>
        ) : (
          <VStack spacing={0} divider={<Divider />}>
            {paginatedAlerts.map((alert) => (
              <Box 
                key={alert.id} 
                p={4} 
                width="100%"
                borderBottomWidth="1px"
                borderColor={borderColor}
                _hover={{ bg: useColorModeValue('gray.50', 'gray.700') }}
                transition="background 0.2s"
              >
                <HStack justify="space-between" mb={2}>
                  <HStack>
                    <Badge colorScheme={getDecisionColor(alert.decision)}>
                      {alert.decision}
                    </Badge>
                    <Badge colorScheme={getRiskColor(alert.riskLevel)}>
                      Risk: {alert.riskLevel}%
                    </Badge>
                    <Text fontSize="sm" color="gray.500">
                      {formatDistanceToNow(alert.timestamp?.toDate() || new Date(), { addSuffix: true })}
                    </Text>
                  </HStack>
                  <Text fontSize="sm" color="gray.500">
                    Source: {alert.source}
                  </Text>
                </HStack>
                <Text>{alert.message}</Text>
                {alert.metadata && (
                  <Box mt={2} fontSize="sm" color="gray.500">
                    <Text as="span" fontWeight="medium">Details: </Text>
                    <Text as="span">
                      {Object.entries(alert.metadata)
                        .map(([key, value]) => `${key}: ${value}`)
                        .join(', ')}
                    </Text>
                  </Box>
                )}
              </Box>
            ))}
          </VStack>
        )}
      </Box>

      {!isLoading && filteredAlerts.length > 0 && (
        <HStack justify="space-between" p={4} borderTopWidth="1px" borderColor={borderColor}>
          <Text fontSize="sm" color="gray.500">
            Showing {filteredAlerts.length === 0 ? 0 : (page - 1) * ITEMS_PER_PAGE + 1} to{' '}
            {Math.min(page * ITEMS_PER_PAGE, filteredAlerts.length)} of {filteredAlerts.length} alerts
          </Text>
          <HStack>
            <Button 
              size="sm" 
              isDisabled={page === 1}
              onClick={() => setPage(p => Math.max(1, p - 1))}
            >
              Previous
            </Button>
            <HStack spacing={1}>
              {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                // Show first, last, and pages around current page
                let pageNum;
                if (totalPages <= 5) {
                  pageNum = i + 1;
                } else if (page <= 3) {
                  pageNum = i + 1;
                } else if (page >= totalPages - 2) {
                  pageNum = totalPages - 4 + i;
                } else {
                  pageNum = page - 2 + i;
                }
                
                return (
                  <Button
                    key={pageNum}
                    size="sm"
                    variant={page === pageNum ? 'solid' : 'outline'}
                    onClick={() => setPage(pageNum)}
                  >
                    {pageNum}
                  </Button>
                );
              })}
            </HStack>
            <Button 
              size="sm" 
              isDisabled={page >= totalPages}
              onClick={() => setPage(p => Math.min(totalPages, p + 1))}
            >
              Next
            </Button>
          </HStack>
        </HStack>
      )}

      {/* Filter Drawer */}
      <Drawer isOpen={isOpen} placement="right" onClose={onClose} size="sm">
        <DrawerOverlay />
        <DrawerContent>
          <DrawerCloseButton />
          <DrawerHeader>Filter Alerts</DrawerHeader>
          <DrawerBody>
            <VStack spacing={4} align="stretch">
              <Box>
                <Text mb={2} fontWeight="medium">Decision</Text>
                <Select 
                  name="decision"
                  value={filters.decision}
                  onChange={handleFilterChange}
                  placeholder="All decisions"
                >
                  <option value="ALLOW">Allow</option>
                  <option value="WARN">Warn</option>
                  <option value="BLOCK">Block</option>
                </Select>
              </Box>

              <Box>
                <Text mb={2} fontWeight="medium">Risk Level</Text>
                <HStack>
                  <Input 
                    name="riskMin"
                    type="number"
                    placeholder="Min"
                    min="0"
                    max="100"
                    value={filters.riskMin}
                    onChange={handleFilterChange}
                  />
                  <Text>to</Text>
                  <Input 
                    name="riskMax"
                    type="number"
                    placeholder="Max"
                    min="0"
                    max="100"
                    value={filters.riskMax}
                    onChange={handleFilterChange}
                  />
                </HStack>
              </Box>

              <Box>
                <Text mb={2} fontWeight="medium">Source</Text>
                <Input 
                  name="source"
                  placeholder="Filter by source"
                  value={filters.source}
                  onChange={handleFilterChange}
                />
              </Box>

              <Button 
                colorScheme="blue" 
                mt={4}
                onClick={onClose}
              >
                Apply Filters
              </Button>
              
              <Button 
                variant="outline" 
                onClick={clearFilters}
                leftIcon={<X />}
              >
                Clear All Filters
              </Button>
            </VStack>
          </DrawerBody>
        </DrawerContent>
      </Drawer>
    </Box>
  );
};

export default LiveAlertsPanel;

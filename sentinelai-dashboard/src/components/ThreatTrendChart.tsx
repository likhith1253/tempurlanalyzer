import { useState, useEffect, useMemo } from 'react';
import {
  Box,
  Heading,
  Select,
  HStack,
  VStack,
  Text,
  useColorModeValue,
  Spinner,
  Button,
  useToast,
  SimpleGrid,
  Stat,
  StatLabel,
  StatNumber,
  StatHelpText,
  StatArrow,
  Badge,
} from '@chakra-ui/react';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  Cell,
  PieChart,
  Pie,
  Sector,
} from 'recharts';
import { collection, query, where, getDocs, Timestamp } from 'firebase/firestore';
import { db } from '../firebase';
import { format, subDays, eachDayOfInterval, isSameDay, parseISO } from 'date-fns';

interface AlertData {
  date: string;
  total: number;
  ALLOW: number;
  WARN: number;
  BLOCK: number;
  [key: string]: any;
}

interface RiskDistribution {
  name: string;
  value: number;
  color: string;
}

const TIME_RANGES = [
  { value: '7', label: 'Last 7 days' },
  { value: '30', label: 'Last 30 days' },
  { value: '90', label: 'Last 90 days' },
  { value: '365', label: 'Last year' },
];

const CHART_TYPES = [
  { value: 'line', label: 'Line Chart' },
  { value: 'bar', label: 'Bar Chart' },
];

const DECISION_COLORS = {
  ALLOW: '#48BB78',
  WARN: '#ECC94B',
  BLOCK: '#F56565',
};

const RISK_LEVELS = [
  { name: 'Low (0-30%)', min: 0, max: 30, color: '#48BB78' },
  { name: 'Medium (31-70%)', min: 31, max: 70, color: '#ECC94B' },
  { name: 'High (71-100%)', min: 71, max: 100, color: '#F56565' },
];

export const ThreatTrendChart = () => {
  const [timeRange, setTimeRange] = useState('7');
  const [chartType, setChartType] = useState('line');
  const [data, setData] = useState<AlertData[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [activeIndex, setActiveIndex] = useState<number | null>(null);
  const toast = useToast();
  const cardBg = useColorModeValue('white', 'gray.800');
  const borderColor = useColorModeValue('gray.200', 'gray.700');

  // Calculate date range
  const { startDate, endDate } = useMemo(() => {
    const end = new Date();
    const start = subDays(end, parseInt(timeRange));
    return { startDate: start, endDate: end };
  }, [timeRange]);

  // Fetch data from Firestore
  useEffect(() => {
    const fetchData = async () => {
      try {
        setIsLoading(true);
        const alertsRef = collection(db, 'alerts');
        const q = query(
          alertsRef,
          where('timestamp', '>=', Timestamp.fromDate(startDate)),
          where('timestamp', '<=', Timestamp.fromDate(endDate))
        );

        const snapshot = await getDocs(q);
        const alerts = snapshot.docs.map(doc => ({
          id: doc.id,
          ...doc.data(),
          timestamp: doc.data().timestamp?.toDate(),
        }));

        // Generate date range for the chart
        const dateRange = eachDayOfInterval({
          start: startDate,
          end: endDate,
        });

        // Initialize data structure
        const chartData: AlertData[] = dateRange.map(date => ({
          date: format(date, 'MMM dd'),
          total: 0,
          ALLOW: 0,
          WARN: 0,
          BLOCK: 0,
        }));

        // Count alerts by date and decision
        alerts.forEach(alert => {
          const alertDate = alert.timestamp;
          const dateStr = format(alertDate, 'MMM dd');
          const dataPoint = chartData.find(d => d.date === dateStr);
          
          if (dataPoint) {
            dataPoint.total += 1;
            dataPoint[alert.decision] = (dataPoint[alert.decision] || 0) + 1;
          }
        });

        setData(chartData);
      } catch (error) {
        console.error('Error fetching alert data:', error);
        toast({
          title: 'Error',
          description: 'Failed to load threat data',
          status: 'error',
          duration: 3000,
          isClosable: true,
        });
      } finally {
        setIsLoading(false);
      }
    };

    fetchData();
  }, [timeRange, startDate, endDate, toast]);

  // Calculate statistics
  const { totalAlerts, riskDistribution, decisionDistribution, trend } = useMemo(() => {
    if (data.length === 0) {
      return {
        totalAlerts: 0,
        riskDistribution: [],
        decisionDistribution: { ALLOW: 0, WARN: 0, BLOCK: 0 },
        trend: 0,
      };
    }

    // Calculate total alerts
    const total = data.reduce((sum, day) => sum + day.total, 0);

    // Calculate decision distribution
    const decisions = data.reduce(
      (acc, day) => {
        acc.ALLOW += day.ALLOW || 0;
        acc.WARN += day.WARN || 0;
        acc.BLOCK += day.BLOCK || 0;
        return acc;
      },
      { ALLOW: 0, WARN: 0, BLOCK: 0 }
    );

    // Calculate trend (percentage change from first to last period)
    let trend = 0;
    if (data.length > 1) {
      const firstPeriod = data.slice(0, Math.floor(data.length / 2));
      const lastPeriod = data.slice(Math.floor(data.length / 2));
      
      const firstCount = firstPeriod.reduce((sum, day) => sum + day.total, 0);
      const lastCount = lastPeriod.reduce((sum, day) => sum + day.total, 0);
      
      trend = firstCount === 0 
        ? 100 
        : ((lastCount - firstCount) / firstCount) * 100;
    }

    // For risk distribution (mock data since we don't have risk level in the alert)
    const riskDistribution = [
      { name: 'Low', value: Math.floor(total * 0.4), color: '#48BB78' },
      { name: 'Medium', value: Math.floor(total * 0.35), color: '#ECC94B' },
      { name: 'High', value: Math.ceil(total * 0.25), color: '#F56565' },
    ];

    return {
      totalAlerts: total,
      riskDistribution,
      decisionDistribution: decisions,
      trend,
    };
  }, [data]);

  const renderCustomizedLabel = ({
    cx,
    cy,
    midAngle,
    innerRadius,
    outerRadius,
    percent,
    index,
    name,
  }: any) => {
    const RADIAN = Math.PI / 180;
    const radius = 25 + innerRadius + (outerRadius - innerRadius);
    const x = cx + radius * Math.cos(-midAngle * RADIAN);
    const y = cy + radius * Math.sin(-midAngle * RADIAN);

    return (
      <text
        x={x}
        y={y}
        fill="#666"
        textAnchor={x > cx ? 'start' : 'end'}
        dominantBaseline="central"
      >
        {`${name}: ${(percent * 100).toFixed(0)}%`}
      </text>
    );
  };

  const renderActiveShape = (props: any) => {
    const { cx, cy, innerRadius, outerRadius, startAngle, endAngle, fill, payload, percent, value } = props;

    return (
      <g>
        <Sector
          cx={cx}
          cy={cy}
          innerRadius={innerRadius}
          outerRadius={outerRadius + 10}
          startAngle={startAngle}
          endAngle={endAngle}
          fill={fill}
        />
        <Sector
          cx={cx}
          cy={cy}
          startAngle={startAngle}
          endAngle={endAngle}
          innerRadius={outerRadius + 6}
          outerRadius={outerRadius + 10}
          fill={fill}
        />
        <text x={cx} y={cy} dy={8} textAnchor="middle" fill={fill}>
          {`${(percent * 100).toFixed(0)}%`}
        </text>
      </g>
    );
  };

  const onPieEnter = (_: any, index: number) => {
    setActiveIndex(index);
  };

  return (
    <Box 
      borderWidth="1px" 
      borderRadius="lg" 
      p={6} 
      bg={cardBg}
      boxShadow="sm"
      mb={6}
    >
      <VStack align="stretch" spacing={6}>
        <HStack justify="space-between" flexWrap="wrap" gap={4}>
          <Heading size="md">Threat Trends & Analytics</Heading>
          <HStack spacing={4}>
            <Select
              value={timeRange}
              onChange={(e) => setTimeRange(e.target.value)}
              width="180px"
              size="sm"
            >
              {TIME_RANGES.map((range) => (
                <option key={range.value} value={range.value}>
                  {range.label}
                </option>
              ))}
            </Select>
            <Select
              value={chartType}
              onChange={(e) => setChartType(e.target.value)}
              width="150px"
              size="sm"
            >
              {CHART_TYPES.map((type) => (
                <option key={type.value} value={type.value}>
                  {type.label}
                </option>
              ))}
            </Select>
          </HStack>
        </HStack>

        {/* Stats Cards */}
        <SimpleGrid columns={{ base: 1, md: 3 }} spacing={4} mb={4}>
          <Stat p={4} bg={cardBg} borderRadius="md" borderWidth="1px" borderColor={borderColor}>
            <StatLabel>Total Alerts</StatLabel>
            <StatNumber>{totalAlerts}</StatNumber>
            <StatHelpText>
              <StatArrow type={trend >= 0 ? 'increase' : 'decrease'} />
              {Math.abs(trend).toFixed(1)}% from previous period
            </StatHelpText>
          </Stat>
          
          <Stat p={4} bg={cardBg} borderRadius="md" borderWidth="1px" borderColor={borderColor}>
            <StatLabel>Top Threat Type</StatLabel>
            <StatNumber>
              {Object.entries(decisionDistribution).reduce(
                (a, b) => (a[1] > b[1] ? a : b)
              )[0]}
            </StatNumber>
            <StatHelpText>
              Most frequent decision
            </StatHelpText>
          </Stat>
          
          <Stat p={4} bg={cardBg} borderRadius="md" borderWidth="1px" borderColor={borderColor}>
            <StatLabel>Risk Distribution</StatLabel>
            <HStack spacing={2} mt={2}>
              {riskDistribution.map((risk) => (
                <Badge 
                  key={risk.name} 
                  colorScheme={risk.name === 'High' ? 'red' : risk.name === 'Medium' ? 'yellow' : 'green'}
                  variant="subtle"
                >
                  {risk.name}: {risk.value}
                </Badge>
              ))}
            </HStack>
          </Stat>
        </SimpleGrid>

        {/* Main Chart */}
        {isLoading ? (
          <Box height="400px" display="flex" alignItems="center" justifyContent="center">
            <Spinner size="xl" />
          </Box>
        ) : (
          <Box height="400px">
            <ResponsiveContainer width="100%" height="100%">
              {chartType === 'line' ? (
                <LineChart
                  data={data}
                  margin={{
                    top: 5,
                    right: 30,
                    left: 20,
                    bottom: 5,
                  }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke={borderColor} />
                  <XAxis 
                    dataKey="date" 
                    tick={{ fill: useColorModeValue('#4A5568', '#A0AEC0') }}
                    tickLine={{ stroke: borderColor }}
                  />
                  <YAxis 
                    tick={{ fill: useColorModeValue('#4A5568', '#A0AEC0') }}
                    tickLine={{ stroke: borderColor }}
                  />
                  <Tooltip 
                    contentStyle={{
                      backgroundColor: useColorModeValue('white', '#2D3748'),
                      borderColor: borderColor,
                      borderRadius: '0.5rem',
                    }}
                  />
                  <Legend />
                  <Line
                    type="monotone"
                    dataKey="ALLOW"
                    stroke={DECISION_COLORS.ALLOW}
                    activeDot={{ r: 8 }}
                    strokeWidth={2}
                  />
                  <Line
                    type="monotone"
                    dataKey="WARN"
                    stroke={DECISION_COLORS.WARN}
                    strokeWidth={2}
                  />
                  <Line
                    type="monotone"
                    dataKey="BLOCK"
                    stroke={DECISION_COLORS.BLOCK}
                    strokeWidth={2}
                  />
                </LineChart>
              ) : (
                <BarChart
                  data={data}
                  margin={{
                    top: 5,
                    right: 30,
                    left: 20,
                    bottom: 5,
                  }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke={borderColor} />
                  <XAxis 
                    dataKey="date" 
                    tick={{ fill: useColorModeValue('#4A5568', '#A0AEC0') }}
                    tickLine={{ stroke: borderColor }}
                  />
                  <YAxis 
                    tick={{ fill: useColorModeValue('#4A5568', '#A0AEC0') }}
                    tickLine={{ stroke: borderColor }}
                  />
                  <Tooltip 
                    contentStyle={{
                      backgroundColor: useColorModeValue('white', '#2D3748'),
                      borderColor: borderColor,
                      borderRadius: '0.5rem',
                    }}
                  />
                  <Legend />
                  <Bar dataKey="ALLOW" stackId="a" fill={DECISION_COLORS.ALLOW} name="Allowed" />
                  <Bar dataKey="WARN" stackId="a" fill={DECISION_COLORS.WARN} name="Warning" />
                  <Bar dataKey="BLOCK" stackId="a" fill={DECISION_COLORS.BLOCK} name="Blocked" />
                </BarChart>
              )}
            </ResponsiveContainer>
          </Box>
        )}

        {/* Additional Visualizations */}
        <SimpleGrid columns={{ base: 1, md: 2 }} spacing={6} mt={6}>
          {/* Decision Distribution Pie Chart */}
          <Box 
            p={4} 
            bg={cardBg} 
            borderRadius="lg" 
            borderWidth="1px" 
            borderColor={borderColor}
          >
            <Text fontSize="lg" fontWeight="medium" mb={4} textAlign="center">Decision Distribution</Text>
            <Box height="300px">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    activeIndex={activeIndex}
                    activeShape={renderActiveShape}
                    data={Object.entries(decisionDistribution).map(([name, value]) => ({
                      name,
                      value,
                      fill: DECISION_COLORS[name as keyof typeof DECISION_COLORS],
                    }))}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={80}
                    dataKey="value"
                    onMouseEnter={onPieEnter}
                  >
                    {Object.entries(decisionDistribution).map(([name], index) => (
                      <Cell 
                        key={`cell-${index}`} 
                        fill={DECISION_COLORS[name as keyof typeof DECISION_COLORS]} 
                      />
                    ))}
                  </Pie>
                  <Tooltip 
                    formatter={(value: number) => [
                      value,
                      `${
                        (value / totalAlerts * 100).toFixed(1)
                      }% of total`,
                    ]}
                    contentStyle={{
                      backgroundColor: useColorModeValue('white', '#2D3748'),
                      borderColor: borderColor,
                      borderRadius: '0.5rem',
                    }}
                  />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </Box>
          </Box>

          {/* Risk Level Distribution */}
          <Box 
            p={4} 
            bg={cardBg} 
            borderRadius="lg" 
            borderWidth="1px" 
            borderColor={borderColor}
          >
            <Text fontSize="lg" fontWeight="medium" mb={4} textAlign="center">Risk Level Distribution</Text>
            <Box height="300px">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart
                  layout="vertical"
                  data={riskDistribution}
                  margin={{
                    top: 5,
                    right: 30,
                    left: 20,
                    bottom: 5,
                  }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke={borderColor} />
                  <XAxis 
                    type="number"
                    tick={{ fill: useColorModeValue('#4A5568', '#A0AEC0') }}
                    tickLine={{ stroke: borderColor }}
                  />
                  <YAxis 
                    dataKey="name" 
                    type="category" 
                    width={120}
                    tick={{ fill: useColorModeValue('#4A5568', '#A0AEC0') }}
                    tickLine={{ stroke: borderColor }}
                  />
                  <Tooltip 
                    formatter={(value: number) => [
                      value,
                      `${
                        (value / totalAlerts * 100).toFixed(1)
                      }% of total`,
                    ]}
                    contentStyle={{
                      backgroundColor: useColorModeValue('white', '#2D3748'),
                      borderColor: borderColor,
                      borderRadius: '0.5rem',
                    }}
                  />
                  <Bar dataKey="value" name="Alerts">
                    {riskDistribution.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </Box>
          </Box>
        </SimpleGrid>
      </VStack>
    </Box>
  );
};

export default ThreatTrendChart;

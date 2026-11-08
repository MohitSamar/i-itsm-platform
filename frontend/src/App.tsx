import React, { useState, useEffect, useCallback } from 'react';
import {
  ShieldAlert, CheckCircle2, Clock, AlertTriangle, Search, PlusCircle,
  UserCheck, Activity, Layers, Bell, RefreshCw, Terminal, ArrowRight,
  GitMerge, Sparkles, Lock, Server, Award, Sliders, FileText, XCircle
} from 'lucide-react';

interface User {
  id: string;
  name: string;
  email: string;
  role: 'EMPLOYEE' | 'TECHNICIAN' | 'ADMINISTRATOR';
  department?: string;
  officeLocation?: string;
  skills?: string[];
  currentWorkload?: number;
  maxCapacity?: number;
  escalationRole?: string;
}

interface ITService {
  id: string;
  serviceCode: string;
  serviceName: string;
  defaultCategory: string;
  businessCriticality: string;
  operationalStatus: 'OPERATIONAL' | 'DEGRADED' | 'OUTAGE';
  dependencies: ITService[];
}

interface AuditRecord {
  id: string;
  incidentKey: string;
  timestamp: string;
  actorName: string;
  action: string;
  previousState: string;
  newState: string;
  description: string;
}

interface Incident {
  id: string;
  incidentKey: string;
  title: string;
  description: string;
  reporter: User;
  affectedService: ITService;
  assignedTechnician?: User;
  category: string;
  impact: string;
  urgency: string;
  priority: 'P1' | 'P2' | 'P3' | 'P4';
  status: string;
  createdAt: string;
  responseDeadline: string;
  resolutionDeadline: string;
  acknowledgedAt?: string;
  resolvedAt?: string;
  closedAt?: string;
  responseSlaViolated: boolean;
  resolutionSlaViolated: boolean;
  slaRiskWarning: boolean;
  escalationLevel: number;
  duplicateOfIncidentKey?: string;
  correlatedRootIncidentKey?: string;
  investigationNotes: string;
  resolutionDetails: string;
  reporterFeedback?: string;
  feedbackRating?: number;
  auditTrail: AuditRecord[];
}

interface SLAPolicy {
  priority: string;
  responseTargetMinutes: number;
  resolutionTargetMinutes: number;
}

interface NotificationItem {
  id: string;
  incidentKey: string;
  recipient: string;
  channel: string;
  eventType: string;
  message: string;
  timestamp: string;
}

const LIFECYCLE_STEPS = [
  'NEW', 'CLASSIFIED', 'ASSIGNED', 'ACKNOWLEDGED', 'IN_PROGRESS', 'RESOLVED', 'VERIFIED', 'CLOSED'
];

const CATEGORIES = [
  'APPLICATION', 'DATABASE', 'NETWORK', 'AUTHENTICATION', 'CLOUD', 'SECURITY', 'STORAGE', 'HARDWARE', 'SOFTWARE'
];

const ESCALATION_LADDER = ['Technician (L1)', 'Team Lead (L2)', 'System Administrator (L3)', 'IT Manager (L4)'];

export default function App() {
  const [users, setUsers] = useState<User[]>([]);
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [services, setServices] = useState<ITService[]>([]);
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [selectedIncident, setSelectedIncident] = useState<Incident | null>(null);
  const [suggestedResolutions, setSuggestedResolutions] = useState<string[]>([]);
  const [analytics, setAnalytics] = useState<any>(null);
  const [slaPolicies, setSlaPolicies] = useState<SLAPolicy[]>([]);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [banner, setBanner] = useState<{ type: 'success' | 'error' | 'info'; text: string } | null>(null);

  // Employee Form State
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [serviceId, setServiceId] = useState('');
  const [impact, setImpact] = useState('HIGH');
  const [urgency, setUrgency] = useState('HIGH');
  const [category, setCategory] = useState('');
  const [aiSuggestedCategory, setAiSuggestedCategory] = useState('');

  // Search & 7-Filter State
  const [searchQuery, setSearchQuery] = useState('');
  const [filterStatus, setFilterStatus] = useState('');
  const [filterPriority, setFilterPriority] = useState('');
  const [filterCategory, setFilterCategory] = useState('');
  const [filterServiceId, setFilterServiceId] = useState('');
  const [filterTechId, setFilterTechId] = useState('');
  const [filterSlaViolated, setFilterSlaViolated] = useState('');

  // Technician & Feedback Inputs
  const [techNote, setTechNote] = useState('');
  const [resolutionText, setResolutionText] = useState('');
  const [feedbackRating, setFeedbackRating] = useState(5);
  const [feedbackComment, setFeedbackComment] = useState('Issue resolved quickly and service restored.');

  const showToast = (type: 'success' | 'error' | 'info', text: string) => {
    setBanner({ type, text });
    setTimeout(() => setBanner(null), 6000);
  };

  const fetchAllData = useCallback(async () => {
    try {
      const params = new URLSearchParams();
      if (searchQuery) params.append('query', searchQuery);
      if (filterStatus) params.append('status', filterStatus);
      if (filterPriority) params.append('priority', filterPriority);
      if (filterCategory) params.append('category', filterCategory);
      if (filterServiceId) params.append('serviceId', filterServiceId);
      if (filterTechId) params.append('technicianId', filterTechId);
      if (filterSlaViolated !== '') params.append('slaViolated', filterSlaViolated);

      const [uRes, sRes, iRes, aRes, slaRes, nRes] = await Promise.all([
        fetch('/api/users'),
        fetch('/api/services'),
        fetch(`/api/incidents?${params.toString()}`),
        fetch('/api/dashboard/analytics'),
        fetch('/api/sla-policies'),
        fetch('/api/notifications'),
      ]);

      const [uData, sData, iData, aData, slaData, nData] = await Promise.all([
        uRes.json(), sRes.json(), iRes.json(), aRes.json(), slaRes.json(), nRes.json()
      ]);

      setUsers(uData);
      setServices(sData);
      setIncidents(iData);
      setAnalytics(aData);
      setSlaPolicies(slaData);
      setNotifications(nData);

      if (!currentUser && uData.length > 0) {
        const defaultEmp = uData.find((u: User) => u.role === 'EMPLOYEE') || uData[0];
        setCurrentUser(defaultEmp);
      }
      if (!serviceId && sData.length > 0) {
        setServiceId(sData[0].id);
      }
    } catch (err: any) {
      showToast('error', 'Failed to reach Backend API (:8080). Ensure Spring Boot is running.');
    }
  }, [searchQuery, filterStatus, filterPriority, filterCategory, filterServiceId, filterTechId, filterSlaViolated, currentUser, serviceId]);

  useEffect(() => {
    fetchAllData();
  }, [fetchAllData]);

  const loadIncidentDetails = async (incidentKey: string) => {
    const res = await fetch(`/api/incidents/${incidentKey}`);
    if (res.ok) {
      const data = await res.json();
      setSelectedIncident(data.incident);
      setSuggestedResolutions(data.suggestedResolutions || []);
    }
  };

  // Auto-suggest category as Employee types
  useEffect(() => {
    const timer = setTimeout(async () => {
      if (title.trim().length > 4 || description.trim().length > 6) {
        const res = await fetch('/api/incidents/suggest-category', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ title, description }),
        });
        if (res.ok) {
          const data = await res.json();
          setAiSuggestedCategory(data.suggestedCategory);
        }
      }
    }, 350);
    return () => clearTimeout(timer);
  }, [title, description]);

  const handleCreateIncident = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentUser) return;
    try {
      const res = await fetch('/api/incidents', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          title,
          description,
          reporterId: currentUser.id,
          serviceId,
          impact,
          urgency,
          category: category || aiSuggestedCategory || '',
        }),
      });
      const data = await res.json();
      if (!res.ok) {
        showToast('error', `[${data.errorCode}] ${data.message}`);
        return;
      }
      setTitle('');
      setDescription('');
      setCategory('');
      showToast(
        'success',
        `Created ${data.incidentKey} | Priority: ${data.priority} | Assigned to: ${data.assignedTechnician ? data.assignedTechnician.name : 'Queue'}${data.duplicateOfIncidentKey ? ' | Flagged Duplicate of ' + data.duplicateOfIncidentKey : ''}`
      );
      await fetchAllData();
      await loadIncidentDetails(data.incidentKey);
    } catch (err: any) {
      showToast('error', err.message);
    }
  };

  const handleStateTransition = async (incidentKey: string, targetStatus: string, notes: string = '') => {
    try {
      const res = await fetch(`/api/incidents/${incidentKey}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          targetStatus,
          actorName: currentUser?.name || 'System User',
          notes,
        }),
      });
      const data = await res.json();
      if (!res.ok) {
        showToast('error', `OOP State Guard Blocked Transition: [${data.errorCode}] ${data.message}`);
        return;
      }
      showToast('success', `Incident ${incidentKey} transitioned to ${targetStatus}`);
      setResolutionText('');
      await fetchAllData();
      await loadIncidentDetails(incidentKey);
    } catch (err: any) {
      showToast('error', err.message);
    }
  };

  const handleAddNote = async (incidentKey: string) => {
    if (!techNote.trim()) return;
    const res = await fetch(`/api/incidents/${incidentKey}/notes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ actorName: currentUser?.name || 'Technician', note: techNote }),
    });
    if (res.ok) {
      setTechNote('');
      showToast('info', 'Investigation note recorded in Audit Trail.');
      await loadIncidentDetails(incidentKey);
      await fetchAllData();
    }
  };

  const handleFeedback = async (incidentKey: string) => {
    const res = await fetch(`/api/incidents/${incidentKey}/feedback`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ rating: feedbackRating, comment: feedbackComment }),
    });
    if (res.ok) {
      showToast('success', 'Feedback recorded. Thank you!');
      await loadIncidentDetails(incidentKey);
      await fetchAllData();
    }
  };

  const handleSwitchStrategy = async (strategyKey: string) => {
    const res = await fetch('/api/admin/assignment-strategy', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ strategyKey }),
    });
    if (res.ok) {
      showToast('info', `Runtime Strategy switched to ${strategyKey} (Strategy Pattern)`);
      await fetchAllData();
    }
  };

  const handleSimulateSlaBreach = async () => {
    const res = await fetch('/api/admin/sla-simulate', { method: 'POST' });
    if (res.ok) {
      const data = await res.json();
      showToast('error', `SLA Watchdog executed (+3h simulation): ${data.escalatedOrFlaggedCount} active incident(s) flagged/escalated.`);
      await fetchAllData();
      if (selectedIncident) {
        await loadIncidentDetails(selectedIncident.incidentKey);
      }
    }
  };

  const handleManualAssign = async (incidentKey: string, technicianId: string) => {
    if (!technicianId) return;
    const res = await fetch(`/api/incidents/${incidentKey}/assign`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ technicianId, actorName: currentUser?.name || 'Administrator' }),
    });
    const data = await res.json();
    if (!res.ok) {
      showToast('error', `[${data.errorCode}] ${data.message}`);
      return;
    }
    showToast('success', `Manually assigned ${incidentKey} to ${data.assignedTechnician?.name}`);
    await fetchAllData();
    await loadIncidentDetails(incidentKey);
  };

  const priorityBadge = (p: string) => {
    const map: Record<string, string> = {
      P1: 'bg-red-500/20 text-red-300 border-red-500/40',
      P2: 'bg-orange-500/20 text-orange-300 border-orange-500/40',
      P3: 'bg-yellow-500/20 text-yellow-300 border-yellow-500/40',
      P4: 'bg-sky-500/20 text-sky-300 border-sky-500/40',
    };
    return map[p] || 'bg-slate-700 text-slate-200';
  };

  const technicians = users.filter((u) => u.role === 'TECHNICIAN');

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      {/* TOP NAVIGATION & ROLE SWITCHER */}
      <header className="border-b border-slate-800 bg-slate-900/80 backdrop-blur sticky top-0 z-30">
        <div className="max-w-7xl mx-auto px-4 py-3 flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-indigo-600/20 border border-indigo-500/40 text-indigo-400">
              <ShieldAlert className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-lg font-bold tracking-tight text-white">I-ITSM Enterprise Platform</h1>
                <span className="text-xs px-2 py-0.5 rounded-full bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 font-medium">
                  CS F213 OOP
                </span>
              </div>
              <p className="text-xs text-slate-400">
                Intelligent IT Service Management & Incident Resolution System
              </p>
            </div>
          </div>

          {/* ROLE-BASED PERSONA SWITCHER */}
          <div className="flex items-center gap-3 flex-wrap">
            <span className="text-xs text-slate-400 font-medium">Active Persona (Role GUI):</span>
            <select
              value={currentUser?.id || ''}
              onChange={(e) => {
                const found = users.find((u) => u.id === e.target.value) || null;
                setCurrentUser(found);
                showToast('info', `Switched GUI to ${found?.role} view: ${found?.name}`);
              }}
              className="bg-slate-800 border border-slate-700 text-sm rounded-lg px-3 py-1.5 text-white focus:outline-none focus:border-indigo-500"
            >
              {users.map((u) => (
                <option key={u.id} value={u.id}>
                  [{u.role}] {u.name}
                </option>
              ))}
            </select>

            <button
              onClick={fetchAllData}
              className="flex items-center gap-1.5 text-xs bg-slate-800 hover:bg-slate-700 border border-slate-700 px-3 py-1.5 rounded-lg text-slate-200 cursor-pointer"
            >
              <RefreshCw className="w-3.5 h-3.5" /> Sync
            </button>
          </div>
        </div>
      </header>

      {/* LIVE TOAST BANNER */}
      {banner && (
        <div className="max-w-7xl mx-auto px-4 mt-3 w-full">
          <div
            className={`flex items-center justify-between px-4 py-3 rounded-xl border text-sm font-medium ${
              banner.type === 'error'
                ? 'bg-red-950/80 border-red-500/50 text-red-200'
                : banner.type === 'success'
                ? 'bg-emerald-950/80 border-emerald-500/50 text-emerald-200'
                : 'bg-indigo-950/80 border-indigo-500/50 text-indigo-200'
            }`}
          >
            <span>{banner.text}</span>
            <button onClick={() => setBanner(null)} className="text-xs opacity-75 hover:opacity-100">
              Dismiss
            </button>
          </div>
        </div>
      )}

      <main className="max-w-7xl mx-auto px-4 py-6 flex-1 w-full space-y-6">
        {/* ROLE BANNER */}
        {currentUser && (
          <div className="bg-gradient-to-r from-slate-900 via-indigo-950/40 to-slate-900 border border-slate-800 rounded-2xl p-4 flex flex-wrap items-center justify-between gap-4">
            <div className="flex items-center gap-3">
              <div className="p-2.5 rounded-xl bg-slate-800 border border-slate-700 text-indigo-400">
                <UserCheck className="w-5 h-5" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-semibold text-white">{currentUser.name}</span>
                  <span className="text-xs px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 font-mono">
                    {currentUser.role}
                  </span>
                </div>
                <p className="text-xs text-slate-400 mt-0.5">
                  {currentUser.role === 'EMPLOYEE' && `Department: ${currentUser.department} | Location: ${currentUser.officeLocation}`}
                  {currentUser.role === 'TECHNICIAN' && `Escalation Tier: ${currentUser.escalationRole} | Workload: ${currentUser.currentWorkload}/${currentUser.maxCapacity} | Skills: ${currentUser.skills?.join(', ')}`}
                  {currentUser.role === 'ADMINISTRATOR' && 'Full Enterprise Command Center: SLA Policies, Strategy Pattern Configuration, Service Dependency Graph & Audit Logs'}
                </p>
              </div>
            </div>
            <div className="text-xs text-slate-400 flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-amber-400" />
              Active Assignment Strategy:
              <span className="font-mono text-emerald-400 font-semibold">
                {analytics?.activeAssignmentStrategy || 'SKILL_AND_EXPERIENCE'}
              </span>
            </div>
          </div>
        )}

        {/* ===================================================================
            VIEW 1: ADMINISTRATOR / IT MANAGER COMMAND CENTER
        =================================================================== */}
        {currentUser?.role === 'ADMINISTRATOR' && analytics && (
          <div className="space-y-6">
            {/* KPI CARDS */}
            <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
              <div className="bg-slate-900 border border-slate-800 rounded-xl p-4">
                <p className="text-xs text-slate-400">Total Incidents</p>
                <p className="text-2xl font-bold text-white mt-1">{analytics.totalIncidents}</p>
              </div>
              <div className="bg-slate-900 border border-slate-800 rounded-xl p-4">
                <p className="text-xs text-slate-400">Open Incidents</p>
                <p className="text-2xl font-bold text-amber-400 mt-1">{analytics.openIncidents}</p>
              </div>
              <div className="bg-slate-900 border border-slate-800 rounded-xl p-4">
                <p className="text-xs text-slate-400">Critical (P1)</p>
                <p className="text-2xl font-bold text-red-400 mt-1">{analytics.criticalIncidents}</p>
              </div>
              <div className="bg-slate-900 border border-slate-800 rounded-xl p-4">
                <p className="text-xs text-slate-400">Resolved / Closed</p>
                <p className="text-2xl font-bold text-emerald-400 mt-1">{analytics.resolvedIncidents}</p>
              </div>
              <div className="bg-slate-900 border border-red-900/50 rounded-xl p-4">
                <p className="text-xs text-red-300">SLA Violations</p>
                <p className="text-2xl font-bold text-red-400 mt-1">{analytics.slaViolations}</p>
              </div>
            </div>

            {/* ADMIN CONTROLS: STRATEGY SWITCHER + SLA SIMULATOR + WORKLOAD */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Strategy & SLA Control */}
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-4">
                <div className="flex items-center justify-between">
                  <h2 className="text-sm font-semibold text-white flex items-center gap-2">
                    <Sliders className="w-4 h-4 text-indigo-400" /> Strategy & SLA Governance
                  </h2>
                  <span className="text-[11px] px-2 py-0.5 rounded bg-slate-800 text-slate-300 font-mono">
                    Strategy Pattern
                  </span>
                </div>
                <div>
                  <label className="text-xs text-slate-400 block mb-1.5">
                    Runtime Technician Assignment Strategy:
                  </label>
                  <div className="grid grid-cols-1 gap-2">
                    {(analytics.availableStrategies || []).map((strat: string) => (
                      <button
                        key={strat}
                        onClick={() => handleSwitchStrategy(strat)}
                        className={`text-left px-3 py-2 rounded-lg border text-xs font-mono flex items-center justify-between cursor-pointer ${
                          analytics.activeAssignmentStrategy === strat
                            ? 'bg-indigo-600/20 border-indigo-500 text-indigo-200 font-bold'
                            : 'bg-slate-800/60 border-slate-700 text-slate-300 hover:bg-slate-800'
                        }`}
                      >
                        <span>{strat}</span>
                        {analytics.activeAssignmentStrategy === strat && (
                          <span className="text-[10px] bg-indigo-500 text-white px-1.5 py-0.5 rounded">ACTIVE</span>
                        )}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="pt-3 border-t border-slate-800 space-y-2">
                  <p className="text-xs text-slate-400">
                    SLA Watchdog & Auto-Escalation Demo Trigger:
                  </p>
                  <button
                    onClick={handleSimulateSlaBreach}
                    className="w-full py-2 px-3 rounded-lg bg-red-600/20 hover:bg-red-600/30 border border-red-500/40 text-red-200 text-xs font-semibold flex items-center justify-center gap-2 cursor-pointer"
                  >
                    <AlertTriangle className="w-4 h-4" /> Simulate +3h SLA Breach & Escalation
                  </button>
                </div>
              </div>

              {/* Technician Workload Table */}
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-3">
                <h2 className="text-sm font-semibold text-white flex items-center gap-2">
                  <Activity className="w-4 h-4 text-emerald-400" /> Technician Workload & Capacity
                </h2>
                <div className="space-y-3">
                  {(analytics.technicianWorkload || []).map((t: any) => (
                    <div key={t.id} className="bg-slate-950/60 border border-slate-800 rounded-xl p-3">
                      <div className="flex items-center justify-between text-xs">
                        <span className="font-semibold text-white">{t.name}</span>
                        <span className="font-mono text-slate-300">
                          {t.activeIncidents} / {t.maxCapacity} Active
                        </span>
                      </div>
                      <div className="w-full bg-slate-800 h-2 rounded-full mt-2 overflow-hidden">
                        <div
                          className={`h-full rounded-full ${
                            t.activeIncidents >= 4 ? 'bg-red-500' : t.activeIncidents >= 2 ? 'bg-amber-400' : 'bg-emerald-400'
                          }`}
                          style={{ width: `${Math.min(100, (t.activeIncidents / t.maxCapacity) * 100)}%` }}
                        />
                      </div>
                      <div className="flex flex-wrap gap-1 mt-2">
                        {(t.skills || []).map((sk: string) => (
                          <span key={sk} className="text-[10px] px-1.5 py-0.5 rounded bg-slate-800 text-slate-300 font-mono">
                            {sk}
                          </span>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Service Dependency Graph & Health Monitor */}
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-3">
                <div className="flex items-center justify-between">
                  <h2 className="text-sm font-semibold text-white flex items-center gap-2">
                    <GitMerge className="w-4 h-4 text-sky-400" /> Service Dependency Graph
                  </h2>
                  <span className="text-[10px] px-2 py-0.5 rounded bg-sky-500/20 text-sky-300">
                    Innovation Feature
                  </span>
                </div>
                <div className="space-y-2.5">
                  {services.map((svc) => (
                    <div key={svc.id} className="bg-slate-950/60 border border-slate-800 rounded-xl p-3 text-xs space-y-1.5">
                      <div className="flex items-center justify-between">
                        <span className="font-semibold text-white">{svc.serviceName}</span>
                        <span
                          className={`px-2 py-0.5 rounded font-mono text-[10px] font-bold ${
                            svc.operationalStatus === 'OUTAGE'
                              ? 'bg-red-500/20 text-red-300 border border-red-500/40'
                              : svc.operationalStatus === 'DEGRADED'
                              ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40'
                              : 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40'
                          }`}
                        >
                          {svc.operationalStatus}
                        </span>
                      </div>
                      <div className="text-slate-400 flex items-center justify-between text-[11px]">
                        <span>Code: {svc.serviceCode}</span>
                        <span>Criticality: {svc.businessCriticality}</span>
                      </div>
                      {svc.dependencies && svc.dependencies.length > 0 && (
                        <div className="pt-1 border-t border-slate-800/80 text-[11px] text-indigo-300 flex flex-wrap items-center gap-1">
                          <span className="text-slate-400">Depends On Upstream:</span>
                          {svc.dependencies.map((dep) => (
                            <span key={dep.id} className="px-1.5 py-0.5 rounded bg-indigo-950 border border-indigo-700/50 font-mono">
                              {dep.serviceName} ({dep.operationalStatus})
                            </span>
                          ))}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ===================================================================
            VIEW 2: EMPLOYEE INCIDENT REPORTING PORTAL
        =================================================================== */}
        {currentUser?.role === 'EMPLOYEE' && (
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-base font-semibold text-white flex items-center gap-2">
                <PlusCircle className="w-5 h-5 text-indigo-400" /> Report a New IT Incident
              </h2>
              <span className="text-xs text-slate-400">
                Intelligent Priority Calculation, Duplicate Detection & Auto-Assignment Enabled
              </span>
            </div>
            <form onSubmit={handleCreateIncident} className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="md:col-span-2 space-y-3">
                <div>
                  <label className="text-xs text-slate-300 block mb-1">Incident Summary / Title *</label>
                  <input
                    type="text"
                    required
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    placeholder="e.g., Unable to make payment or complete checkout transaction"
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-indigo-500"
                  />
                </div>
                <div>
                  <label className="text-xs text-slate-300 block mb-1">Detailed Description & Error Symptoms *</label>
                  <textarea
                    rows={3}
                    required
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    placeholder="Describe affected users, error messages, and transaction impact..."
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:border-indigo-500"
                  />
                </div>
              </div>

              <div className="space-y-3 flex flex-col justify-between">
                <div className="space-y-2.5">
                  <div>
                    <label className="text-xs text-slate-300 block mb-1">Affected IT Service *</label>
                    <select
                      value={serviceId}
                      onChange={(e) => setServiceId(e.target.value)}
                      className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-white"
                    >
                      {services.map((s) => (
                        <option key={s.id} value={s.id}>
                          {s.serviceName} ({s.businessCriticality})
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="grid grid-cols-2 gap-2">
                    <div>
                      <label className="text-xs text-slate-300 block mb-1">Business Impact</label>
                      <select
                        value={impact}
                        onChange={(e) => setImpact(e.target.value)}
                        className="w-full bg-slate-950 border border-slate-800 rounded-lg px-2.5 py-2 text-xs text-white"
                      >
                        <option value="LOW">LOW</option>
                        <option value="MEDIUM">MEDIUM</option>
                        <option value="HIGH">HIGH</option>
                        <option value="ENTERPRISE_WIDE">ENTERPRISE_WIDE</option>
                      </select>
                    </div>
                    <div>
                      <label className="text-xs text-slate-300 block mb-1">Urgency</label>
                      <select
                        value={urgency}
                        onChange={(e) => setUrgency(e.target.value)}
                        className="w-full bg-slate-950 border border-slate-800 rounded-lg px-2.5 py-2 text-xs text-white"
                      >
                        <option value="LOW">LOW</option>
                        <option value="MEDIUM">MEDIUM</option>
                        <option value="HIGH">HIGH</option>
                        <option value="IMMEDIATE">IMMEDIATE</option>
                      </select>
                    </div>
                  </div>

                  <div>
                    <label className="text-xs text-slate-300 flex items-center justify-between mb-1">
                      <span>Category</span>
                      {aiSuggestedCategory && (
                        <span className="text-[11px] text-emerald-400 flex items-center gap-1">
                          <Sparkles className="w-3 h-3" /> Auto-Suggested: {aiSuggestedCategory}
                        </span>
                      )}
                    </label>
                    <select
                      value={category}
                      onChange={(e) => setCategory(e.target.value)}
                      className="w-full bg-slate-950 border border-slate-800 rounded-lg px-2.5 py-2 text-xs text-white"
                    >
                      <option value="">Auto-Detect ({aiSuggestedCategory || 'Intelligent Classifier'})</option>
                      {CATEGORIES.map((c) => (
                        <option key={c} value={c}>{c}</option>
                      ))}
                    </select>
                  </div>
                </div>

                <button
                  type="submit"
                  className="w-full py-2.5 px-4 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-xs rounded-xl shadow-lg shadow-indigo-600/20 cursor-pointer"
                >
                  Submit & Auto-Dispatch Incident
                </button>
              </div>
            </form>
          </div>
        )}

        {/* ===================================================================
            7-CRITERIA SEARCH & FILTER BAR (ALL ROLES)
        =================================================================== */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-4 space-y-3">
          <div className="flex items-center justify-between flex-wrap gap-2">
            <h2 className="text-xs font-semibold uppercase tracking-wider text-slate-400 flex items-center gap-2">
              <Search className="w-4 h-4 text-indigo-400" /> Multi-Criteria Incident Search & Filter (7 Criteria)
            </h2>
            <button
              onClick={() => {
                setSearchQuery('');
                setFilterStatus('');
                setFilterPriority('');
                setFilterCategory('');
                setFilterServiceId('');
                setFilterTechId('');
                setFilterSlaViolated('');
              }}
              className="text-xs text-slate-400 hover:text-white cursor-pointer"
            >
              Reset Filters
            </button>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-7 gap-2.5">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="ID or keyword..."
              className="bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-xs text-white"
            />
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="bg-slate-950 border border-slate-800 rounded-lg px-2 py-1.5 text-xs text-white"
            >
              <option value="">All Statuses</option>
              {LIFECYCLE_STEPS.map((st) => (
                <option key={st} value={st}>{st}</option>
              ))}
            </select>
            <select
              value={filterPriority}
              onChange={(e) => setFilterPriority(e.target.value)}
              className="bg-slate-950 border border-slate-800 rounded-lg px-2 py-1.5 text-xs text-white"
            >
              <option value="">All Priorities</option>
              <option value="P1">P1 (Critical)</option>
              <option value="P2">P2 (High)</option>
              <option value="P3">P3 (Medium)</option>
              <option value="P4">P4 (Low)</option>
            </select>
            <select
              value={filterCategory}
              onChange={(e) => setFilterCategory(e.target.value)}
              className="bg-slate-950 border border-slate-800 rounded-lg px-2 py-1.5 text-xs text-white"
            >
              <option value="">All Categories</option>
              {CATEGORIES.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
            </select>
            <select
              value={filterServiceId}
              onChange={(e) => setFilterServiceId(e.target.value)}
              className="bg-slate-950 border border-slate-800 rounded-lg px-2 py-1.5 text-xs text-white"
            >
              <option value="">All IT Services</option>
              {services.map((s) => (
                <option key={s.id} value={s.id}>{s.serviceName}</option>
              ))}
            </select>
            <select
              value={filterTechId}
              onChange={(e) => setFilterTechId(e.target.value)}
              className="bg-slate-950 border border-slate-800 rounded-lg px-2 py-1.5 text-xs text-white"
            >
              <option value="">All Technicians</option>
              {technicians.map((t) => (
                <option key={t.id} value={t.id}>{t.name}</option>
              ))}
            </select>
            <select
              value={filterSlaViolated}
              onChange={(e) => setFilterSlaViolated(e.target.value)}
              className="bg-slate-950 border border-slate-800 rounded-lg px-2 py-1.5 text-xs text-white"
            >
              <option value="">All SLA States</option>
              <option value="true">SLA Violated</option>
              <option value="false">Within SLA</option>
            </select>
          </div>
        </div>

        {/* ===================================================================
            MASTER-DETAIL INCIDENT WORKSPACE (QUEUE + LIFECYCLE INSPECTOR)
        =================================================================== */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* INCIDENT LIST QUEUE (LEFT 5 COLS) */}
          <div className="lg:col-span-5 space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-semibold text-white">
                Incident Queue ({incidents.length})
              </h3>
              <span className="text-xs text-slate-400">Click an incident to inspect lifecycle & audit trail</span>
            </div>

            <div className="space-y-2.5 max-h-[680px] overflow-y-auto pr-1">
              {incidents.map((inc) => (
                <div
                  key={inc.id}
                  onClick={() => loadIncidentDetails(inc.incidentKey)}
                  className={`p-4 rounded-xl border transition cursor-pointer ${
                    selectedIncident?.incidentKey === inc.incidentKey
                      ? 'bg-indigo-950/40 border-indigo-500'
                      : 'bg-slate-900 border-slate-800 hover:border-slate-700'
                  }`}
                >
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-xs font-bold text-indigo-400">{inc.incidentKey}</span>
                      <span className={`text-[11px] px-2 py-0.5 rounded border font-semibold ${priorityBadge(inc.priority)}`}>
                        {inc.priority}
                      </span>
                      <span className="text-[11px] px-2 py-0.5 rounded bg-slate-800 text-slate-300 font-mono">
                        {inc.status}
                      </span>
                    </div>
                    {(inc.responseSlaViolated || inc.resolutionSlaViolated) ? (
                      <span className="text-[10px] px-2 py-0.5 rounded bg-red-500/20 text-red-300 border border-red-500/40 font-semibold">
                        SLA VIOLATED
                      </span>
                    ) : inc.slaRiskWarning ? (
                      <span className="text-[10px] px-2 py-0.5 rounded bg-amber-500/20 text-amber-300 border border-amber-500/40 font-semibold">
                        SLA RISK
                      </span>
                    ) : (
                      <span className="text-[10px] px-2 py-0.5 rounded bg-emerald-500/15 text-emerald-300">
                        SLA OK
                      </span>
                    )}
                  </div>

                  <p className="text-sm font-medium text-white mt-2 line-clamp-1">{inc.title}</p>
                  <div className="flex flex-wrap items-center justify-between text-xs text-slate-400 mt-2 gap-2">
                    <span>Service: {inc.affectedService?.serviceName}</span>
                    <span>Tech: {inc.assignedTechnician ? inc.assignedTechnician.name.split(' ')[0] : 'Unassigned'}</span>
                  </div>

                  {/* INTELLIGENCE BADGES */}
                  {(inc.duplicateOfIncidentKey || inc.correlatedRootIncidentKey || inc.escalationLevel > 0) && (
                    <div className="flex flex-wrap gap-1.5 mt-2.5 pt-2 border-t border-slate-800/80">
                      {inc.duplicateOfIncidentKey && (
                        <span className="text-[10px] px-2 py-0.5 rounded bg-amber-500/20 text-amber-300 border border-amber-500/40">
                          Duplicate of {inc.duplicateOfIncidentKey}
                        </span>
                      )}
                      {inc.correlatedRootIncidentKey && (
                        <span className="text-[10px] px-2 py-0.5 rounded bg-purple-500/20 text-purple-300 border border-purple-500/40">
                          Correlated Root Cause: {inc.correlatedRootIncidentKey}
                        </span>
                      )}
                      {inc.escalationLevel > 0 && (
                        <span className="text-[10px] px-2 py-0.5 rounded bg-red-500/20 text-red-300 border border-red-500/40">
                          Escalated: {ESCALATION_LADDER[inc.escalationLevel]}
                        </span>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* INCIDENT LIFECYCLE, INTELLIGENCE & AUDIT INSPECTOR (RIGHT 7 COLS) */}
          <div className="lg:col-span-7">
            {!selectedIncident ? (
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-12 text-center space-y-2">
                <Terminal className="w-8 h-8 text-slate-500 mx-auto" />
                <p className="text-sm font-medium text-slate-300">Select an incident from the queue to inspect its OOP State Lifecycle, SLA timers, AI Resolution Recommendations, and Immutable Audit Trail.</p>
              </div>
            ) : (
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-5">
                {/* HEADER */}
                <div className="flex flex-wrap items-start justify-between gap-4 border-b border-slate-800 pb-4">
                  <div>
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-mono text-base font-bold text-indigo-400">
                        {selectedIncident.incidentKey}
                      </span>
                      <span className={`text-xs px-2.5 py-0.5 rounded border font-bold ${priorityBadge(selectedIncident.priority)}`}>
                        {selectedIncident.priority}
                      </span>
                      <span className="text-xs px-2.5 py-0.5 rounded bg-slate-800 text-slate-200 font-mono">
                        {selectedIncident.category}
                      </span>
                      <span className="text-xs px-2.5 py-0.5 rounded bg-indigo-500/20 text-indigo-300 font-mono">
                        State: {selectedIncident.status}
                      </span>
                    </div>
                    <h2 className="text-base font-bold text-white mt-2">{selectedIncident.title}</h2>
                    <p className="text-xs text-slate-300 mt-1">{selectedIncident.description}</p>
                  </div>
                </div>

                {/* 8-STAGE STATE PATTERN LIFECYCLE STEPPER */}
                <div className="space-y-2">
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-semibold text-slate-300">State Pattern Lifecycle Enforcement:</span>
                    <span className="text-slate-400 font-mono">NEW → CLASSIFIED → ASSIGNED → ACKNOWLEDGED → IN_PROGRESS → RESOLVED → VERIFIED → CLOSED</span>
                  </div>
                  <div className="grid grid-cols-4 sm:grid-cols-8 gap-1.5">
                    {LIFECYCLE_STEPS.map((step, idx) => {
                      const currentIdx = LIFECYCLE_STEPS.indexOf(selectedIncident.status);
                      const isDone = idx <= currentIdx;
                      const isCurrent = step === selectedIncident.status;
                      return (
                        <div
                          key={step}
                          className={`text-[10px] font-mono py-1.5 px-1 rounded text-center border ${
                            isCurrent
                              ? 'bg-indigo-600 text-white border-indigo-400 font-bold'
                              : isDone
                              ? 'bg-emerald-950/60 text-emerald-300 border-emerald-700/50'
                              : 'bg-slate-950 text-slate-500 border-slate-800'
                          }`}
                        >
                          {step}
                        </div>
                      );
                    })}
                  </div>
                </div>

                {/* METADATA & SLA DEADLINES */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs bg-slate-950/70 p-3.5 rounded-xl border border-slate-800">
                  <div>
                    <span className="text-slate-400 block">Reporter</span>
                    <span className="font-medium text-white">{selectedIncident.reporter?.name}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block">Affected Service</span>
                    <span className="font-medium text-white">{selectedIncident.affectedService?.serviceName}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block">Assigned Technician</span>
                    <span className="font-medium text-emerald-300">
                      {selectedIncident.assignedTechnician?.name || 'Unassigned'}
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-400 block">Escalation Tier</span>
                    <span className="font-medium text-amber-300">
                      {ESCALATION_LADDER[selectedIncident.escalationLevel]}
                    </span>
                  </div>
                </div>

                {/* ACTION CONSOLE BASED ON ROLE & STATE PATTERN */}
                <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 space-y-3">
                  <div className="flex items-center justify-between">
                    <h4 className="text-xs font-semibold uppercase tracking-wider text-indigo-300">
                      Lifecycle & Role Action Console ({currentUser?.role})
                    </h4>
                    {/* NEGATIVE TEST CASE BUTTON FOR DEMO */}
                    {selectedIncident.status !== 'CLOSED' && selectedIncident.status !== 'VERIFIED' && (
                      <button
                        onClick={() => handleStateTransition(selectedIncident.incidentKey, 'CLOSED', 'Attempting direct close')}
                        className="text-[11px] px-2.5 py-1 rounded bg-red-950/70 hover:bg-red-900 border border-red-700/60 text-red-200 cursor-pointer"
                      >
                        Test State Guard (Try Illegal {selectedIncident.status} → CLOSED)
                      </button>
                    )}
                  </div>

                  {/* TECHNICIAN / ADMIN LIFECYCLE CONTROLS */}
                  {(currentUser?.role === 'TECHNICIAN' || currentUser?.role === 'ADMINISTRATOR') && (
                    <div className="space-y-3">
                      <div className="flex flex-wrap gap-2">
                        {selectedIncident.status === 'ASSIGNED' && (
                          <button
                            onClick={() => handleStateTransition(selectedIncident.incidentKey, 'ACKNOWLEDGED', 'Technician acknowledged incident and verified SLA response.')}
                            className="px-3 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold cursor-pointer"
                          >
                            1. Acknowledge Incident
                          </button>
                        )}
                        {selectedIncident.status === 'ACKNOWLEDGED' && (
                          <button
                            onClick={() => handleStateTransition(selectedIncident.incidentKey, 'IN_PROGRESS', 'Investigation started on affected service.')}
                            className="px-3 py-1.5 rounded-lg bg-amber-600 hover:bg-amber-500 text-white text-xs font-semibold cursor-pointer"
                          >
                            2. Start Investigation (IN_PROGRESS)
                          </button>
                        )}
                      </div>

                      {/* INTELLIGENT RESOLUTION RECOMMENDER */}
                      {suggestedResolutions.length > 0 && (
                        <div className="bg-indigo-950/30 border border-indigo-800/50 rounded-lg p-3 space-y-1.5">
                          <p className="text-xs font-semibold text-indigo-300 flex items-center gap-1.5">
                            <Sparkles className="w-3.5 h-3.5 text-amber-400" /> Historical Resolution Recommendations (Click to Apply):
                          </p>
                          {suggestedResolutions.map((rec, i) => (
                            <button
                              key={i}
                              onClick={() => setResolutionText(rec)}
                              className="block w-full text-left text-xs text-slate-300 hover:text-white bg-slate-900/80 px-2.5 py-1.5 rounded border border-slate-800 cursor-pointer"
                            >
                              {rec}
                            </button>
                          ))}
                        </div>
                      )}

                      {selectedIncident.status === 'IN_PROGRESS' && (
                        <div className="flex gap-2">
                          <input
                            type="text"
                            value={resolutionText}
                            onChange={(e) => setResolutionText(e.target.value)}
                            placeholder="Enter root-cause fix and resolution details..."
                            className="flex-1 bg-slate-900 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-white"
                          />
                          <button
                            onClick={() => handleStateTransition(selectedIncident.incidentKey, 'RESOLVED', resolutionText)}
                            className="px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold cursor-pointer"
                          >
                            3. Mark RESOLVED
                          </button>
                        </div>
                      )}

                      {/* ADD INVESTIGATION NOTE */}
                      <div className="flex gap-2 pt-2 border-t border-slate-800">
                        <input
                          type="text"
                          value={techNote}
                          onChange={(e) => setTechNote(e.target.value)}
                          placeholder="Add investigation log or diagnostic note..."
                          className="flex-1 bg-slate-900 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-white"
                        />
                        <button
                          onClick={() => handleAddNote(selectedIncident.incidentKey)}
                          className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium cursor-pointer"
                        >
                          Add Note
                        </button>
                      </div>

                      {/* ADMIN MANUAL REASSIGNMENT */}
                      {currentUser?.role === 'ADMINISTRATOR' && (
                        <div className="flex items-center gap-2 pt-2 border-t border-slate-800">
                          <span className="text-xs text-slate-400">Manual Reassign:</span>
                          <select
                            onChange={(e) => handleManualAssign(selectedIncident.incidentKey, e.target.value)}
                            defaultValue=""
                            className="bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1 text-xs text-white"
                          >
                            <option value="" disabled>Select Technician...</option>
                            {technicians.map((t) => (
                              <option key={t.id} value={t.id}>
                                {t.name} ({t.currentWorkload}/{t.maxCapacity})
                              </option>
                            ))}
                          </select>
                        </div>
                      )}
                    </div>
                  )}

                  {/* VERIFICATION & CLOSURE (EMPLOYEE OR ADMIN/TECH) */}
                  {selectedIncident.status === 'RESOLVED' && (
                    <div className="flex items-center justify-between bg-emerald-950/30 border border-emerald-700/40 rounded-lg p-3">
                      <div className="text-xs text-emerald-200">
                        <strong>Resolution Provided:</strong> {selectedIncident.resolutionDetails}
                      </div>
                      <button
                        onClick={() => handleStateTransition(selectedIncident.incidentKey, 'VERIFIED', 'Resolution verified by user/QA.')}
                        className="px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold cursor-pointer"
                      >
                        4. Verify Resolution (VERIFIED)
                      </button>
                    </div>
                  )}

                  {selectedIncident.status === 'VERIFIED' && (
                    <div className="space-y-3 bg-indigo-950/30 border border-indigo-700/40 rounded-lg p-3">
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-semibold text-indigo-200">
                          Submit Post-Resolution Feedback & Close Incident
                        </span>
                        <button
                          onClick={async () => {
                            await handleFeedback(selectedIncident.incidentKey);
                            await handleStateTransition(selectedIncident.incidentKey, 'CLOSED', 'Incident formally closed after verification and feedback.');
                          }}
                          className="px-3 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold cursor-pointer"
                        >
                          5. Submit Feedback & Close Incident (CLOSED)
                        </button>
                      </div>
                      <div className="flex gap-2">
                        <select
                          value={feedbackRating}
                          onChange={(e) => setFeedbackRating(Number(e.target.value))}
                          className="bg-slate-900 border border-slate-700 rounded px-2 py-1 text-xs text-amber-300"
                        >
                          <option value={5}>★★★★★ (5/5)</option>
                          <option value={4}>★★★★☆ (4/5)</option>
                          <option value={3}>★★★☆☆ (3/5)</option>
                          <option value={2}>★★☆☆☆ (2/5)</option>
                          <option value={1}>★☆☆☆☆ (1/5)</option>
                        </select>
                        <input
                          type="text"
                          value={feedbackComment}
                          onChange={(e) => setFeedbackComment(e.target.value)}
                          className="flex-1 bg-slate-900 border border-slate-700 rounded px-2.5 py-1 text-xs text-white"
                        />
                      </div>
                    </div>
                  )}
                </div>

                {/* IMMUTABLE AUDIT TRAIL TIMELINE */}
                <div className="space-y-2">
                  <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
                    <FileText className="w-3.5 h-3.5 text-indigo-400" /> Complete Incident Audit Trail ({selectedIncident.auditTrail?.length || 0} Events)
                  </h4>
                  <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
                    {(selectedIncident.auditTrail || []).map((a) => (
                      <div key={a.id} className="bg-slate-950 border border-slate-800/90 rounded-lg p-2.5 text-xs flex items-start justify-between gap-3">
                        <div className="space-y-0.5">
                          <div className="flex items-center gap-2">
                            <span className="font-semibold text-indigo-300">{a.action}</span>
                            <span className="text-slate-400">by {a.actorName}</span>
                            {a.previousState && a.newState && (
                              <span className="font-mono text-[10px] px-1.5 py-0.5 rounded bg-slate-800 text-slate-300">
                                {a.previousState} → {a.newState}
                              </span>
                            )}
                          </div>
                          <p className="text-slate-300">{a.description}</p>
                        </div>
                        <span className="text-[10px] font-mono text-slate-500 whitespace-nowrap">
                          {a.timestamp ? new Date(a.timestamp).toLocaleTimeString() : ''}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* OBSERVER PATTERN MULTI-CHANNEL NOTIFICATION FEED */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-3">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold text-white flex items-center gap-2">
              <Bell className="w-4 h-4 text-amber-400" /> Observer Pattern Multi-Channel Notification Feed (In-App & Simulated SMTP Email)
            </h3>
            <span className="text-xs text-slate-400 font-mono">{notifications.length} dispatches recorded</span>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2.5 max-h-52 overflow-y-auto pr-1">
            {notifications.slice(0, 12).map((n) => (
              <div key={n.id} className="bg-slate-950 border border-slate-800 rounded-xl p-3 text-xs space-y-1">
                <div className="flex items-center justify-between">
                  <span className="font-mono text-[10px] px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-300">
                    {n.channel}
                  </span>
                  <span className="font-mono text-[11px] text-amber-300">{n.eventType}</span>
                  <span className="text-[10px] text-slate-500">To: {n.recipient}</span>
                </div>
                <p className="text-slate-300">{n.message}</p>
              </div>
            ))}
          </div>
        </div>
      </main>
    </div>
  );
}

/**
 * Tạo đề thi từ kho câu hỏi – khớp BE:
 * - GET /api/questions/by-part/{examPartId} (cá nhân: không query; JWT gửi kèm)
 * - POST /api/tests → POST /api/test-parts → POST /api/tests/parts/questions hoặc /parts/random-questions
 *
 * Copy vào project React, đổi import path (styles, hook) cho đúng.
 */
import React, { useEffect, useState } from 'react';
import { Row, Col, Spinner, Alert, Button, Form } from 'react-bootstrap';
import axios from 'axios';
import classNames from 'classnames/bind';
import { toast } from 'react-toastify';
import {
  IoLibraryOutline,
  IoSettingsOutline,
  IoCheckboxOutline,
  IoShuffleOutline,
  IoBookOutline,
  IoTimeOutline,
  IoRocketOutline,
  IoChevronDownOutline,
  IoChevronUpOutline,
} from 'react-icons/io5';
import { useBaseMetaData } from '~/hook/useBaseMetaData';
import styles from './CreateTestFromBankPage.module.scss';

const cx = classNames.bind(styles);

const SELECTION_MODES = {
  MANUAL: 'manual',
  RANDOM: 'random',
};

const defaultPartConfig = () => ({
  mode: SELECTION_MODES.RANDOM,
  randomCount: '',
  selectedIds: [],
  bankQuestions: [],
  loading: false,
  expanded: true,
});

const CreateTestFromBankPage = () => {
  const [testInfo, setTestInfo] = useState({
    title: '',
    description: '',
    durationMinutes: '',
    maxAttempts: '',
    examTypeId: '',
    bannerUrl: '',
    availableFrom: '',
    availableTo: '',
  });

  const [partConfigs, setPartConfigs] = useState({});
  const [loadingSubmit, setLoadingSubmit] = useState(false);
  const [notification, setNotification] = useState({});

  const { examTypes, examParts } = useBaseMetaData(testInfo.examTypeId);

  // BE: GET /api/questions/by-part/{examPartId} (cá nhân: không classId/chapterId; JWT tự gửi)
  useEffect(() => {
    if (!testInfo.examTypeId || !examParts?.length) {
      setPartConfigs({});
      return;
    }
    const initial = {};
    examParts.forEach((p) => {
      initial[p.examPartId] = { ...defaultPartConfig(), expanded: false, loading: true };
    });
    setPartConfigs(initial);

    examParts.forEach((part) => {
      const examPartId = part.examPartId;
      axios
        .get(`/api/questions/by-part/${examPartId}`)
        .then((res) => {
          const list = Array.isArray(res.data) ? res.data : res.data?.data ?? res.data?.questions ?? [];
          setPartConfigs((prev) => ({
            ...prev,
            [examPartId]: { ...prev[examPartId], bankQuestions: list, loading: false },
          }));
        })
        .catch((err) => {
          console.error(err);
          setPartConfigs((prev) => ({
            ...prev,
            [examPartId]: { ...prev[examPartId], bankQuestions: [], loading: false },
          }));
        });
    });
  }, [testInfo.examTypeId, examParts]);

  const handleExamTypeChange = (value) => {
    setTestInfo((prev) => ({ ...prev, examTypeId: value }));
  };

  const updatePartConfig = (examPartId, field, value) => {
    setPartConfigs((prev) => ({
      ...prev,
      [examPartId]: { ...prev[examPartId], [field]: value },
    }));
  };

  const togglePartExpanded = (examPartId) => {
    setPartConfigs((prev) => ({
      ...prev,
      [examPartId]: { ...prev[examPartId], expanded: !prev[examPartId].expanded },
    }));
  };

  /** Nhóm câu theo passage_id (cùng passage = 1 nhóm; không passage = mỗi câu 1 nhóm). */
  const groupQuestionsByPassage = (questions) => {
    if (!questions?.length) return [];
    const map = new Map();
    questions.forEach((q) => {
      const id = q.questionId ?? q.id;
      const passageId = q.passageId ?? q.passage?.passageId ?? null;
      const groupKey = passageId != null ? `passage-${passageId}` : `no-passage-${id}`;
      if (!map.has(groupKey)) {
        map.set(groupKey, { groupKey, passageId, questions: [] });
      }
      map.get(groupKey).questions.push(q);
    });
    return Array.from(map.values());
  };

  /** Bật/tắt cả nhóm (cùng passage): chọn hoặc bỏ chọn toàn bộ câu trong nhóm. */
  const toggleGroup = (examPartId, groupKey) => {
    const cfg = partConfigs[examPartId];
    if (!cfg) return;
    const groups = groupQuestionsByPassage(cfg.bankQuestions);
    const group = groups.find((g) => g.groupKey === groupKey);
    if (!group) return;
    const ids = group.questions.map((q) => q.questionId ?? q.id).filter(Boolean);
    const selectedSet = new Set(cfg.selectedIds || []);
    const allSelected = ids.every((id) => selectedSet.has(id));
    if (allSelected) {
      ids.forEach((id) => selectedSet.delete(id));
    } else {
      ids.forEach((id) => selectedSet.add(id));
    }
    updatePartConfig(examPartId, 'selectedIds', Array.from(selectedSet));
  };

  const isGroupSelected = (examPartId, groupKey) => {
    const cfg = partConfigs[examPartId];
    if (!cfg) return false;
    const groups = groupQuestionsByPassage(cfg.bankQuestions);
    const group = groups.find((g) => g.groupKey === groupKey);
    if (!group) return false;
    const ids = group.questions.map((q) => q.questionId ?? q.id).filter(Boolean);
    const selectedSet = new Set(cfg.selectedIds || []);
    return ids.length > 0 && ids.every((id) => selectedSet.has(id));
  };

  const toggleSelectAll = (examPartId, checked) => {
    const cfg = partConfigs[examPartId];
    if (!cfg) return;
    const ids = (cfg.bankQuestions || [])
      .map((q) => q.questionId ?? q.id)
      .filter(Boolean);
    updatePartConfig(examPartId, 'selectedIds', checked ? ids : []);
  };

  const getPartEffectiveCount = (examPartId) => {
    const cfg = partConfigs[examPartId];
    if (!cfg) return 0;
    if (cfg.mode === SELECTION_MODES.RANDOM) {
      const n = Math.max(0, parseInt(cfg.randomCount, 10) || 0);
      const maxInBank = (cfg.bankQuestions || []).length;
      return Math.min(n, maxInBank);
    }
    return (cfg.selectedIds || []).length;
  };

  const hasPartWithQuestions = (part) => {
    const cfg = partConfigs[part.examPartId];
    if (!cfg) return false;
    if (cfg.mode === SELECTION_MODES.RANDOM) {
      const n = Math.max(0, parseInt(cfg.randomCount, 10) || 0);
      return n > 0 && (cfg.bankQuestions || []).length > 0;
    }
    return (cfg.selectedIds || []).length > 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!testInfo.title?.trim() || !testInfo.examTypeId) {
      setNotification({ type: 'warning', message: 'Vui lòng điền tiêu đề và chọn loại kỳ thi.' });
      return;
    }

    const partsToAdd = examParts.filter(hasPartWithQuestions);
    if (partsToAdd.length === 0) {
      setNotification({ type: 'warning', message: 'Vui lòng cấu hình ít nhất một part có câu hỏi (số câu > 0 hoặc chọn thủ công).' });
      return;
    }

    setLoadingSubmit(true);
    setNotification({});

    try {
      const testRes = await axios.post('/api/tests', {
        title: testInfo.title.trim(),
        description: testInfo.description || null,
        examTypeId: Number(testInfo.examTypeId),
        durationMinutes: testInfo.durationMinutes && Number(testInfo.durationMinutes) > 0 ? Number(testInfo.durationMinutes) : null,
        maxAttempts: testInfo.maxAttempts && Number(testInfo.maxAttempts) > 0 ? Number(testInfo.maxAttempts) : null,
        bannerUrl: testInfo.bannerUrl || null,
        availableFrom: testInfo.availableFrom ? testInfo.availableFrom + ':00' : null,
        availableTo: testInfo.availableTo ? testInfo.availableTo + ':00' : null,
        classId: null,
        chapterId: null,
      });

      const newTestId = testRes.data.testId ?? testRes.data.id;
      if (!newTestId) throw new Error('Không nhận được testId từ server.');

      for (const part of partsToAdd) {
        const cfg = partConfigs[part.examPartId];
        const numQuestions = getPartEffectiveCount(part.examPartId);
        if (numQuestions <= 0) continue;

        const partRes = await axios.post('/api/test-parts', {
          testId: Number(newTestId),
          examPartId: Number(part.examPartId),
          numQuestions,
        });
        const newPartId = partRes.data.testPartId ?? partRes.data.id;
        if (!newPartId) throw new Error(`Không nhận được testPartId cho part ${part.name}.`);

        if (cfg.mode === SELECTION_MODES.RANDOM) {
          await axios.post('/api/tests/parts/random-questions', {
            testPartId: Number(newPartId),
            count: numQuestions,
          });
        } else {
          await axios.post('/api/tests/parts/questions', {
            testPartId: Number(newPartId),
            questionIds: (cfg.selectedIds || []).map(Number),
          });
        }
      }

      setNotification({ type: 'success', message: 'Tạo đề thi từ kho câu hỏi thành công!' });
      setTestInfo((prev) => ({ ...prev, title: '', description: '' }));
      toast.success('Đã tạo đề thi từ kho câu hỏi!');
    } catch (error) {
      const msg = error.response?.data?.message ?? error.response?.data ?? error.message;
      setNotification({ type: 'danger', message: 'Lỗi: ' + (typeof msg === 'string' ? msg : JSON.stringify(msg)) });
    } finally {
      setLoadingSubmit(false);
    }
  };

  const totalSelected = (examParts || []).reduce((sum, p) => sum + getPartEffectiveCount(p.examPartId), 0);
  const hasAnyPartWithQuestions = (examParts || []).some((p) => getPartEffectiveCount(p.examPartId) > 0);

  return (
    <div className={cx('wrapper')}>
      <div className={cx('container')}>
        <header className={cx('header')}>
          <h1 className={cx('title')}>
            <IoLibraryOutline /> Tạo đề thi từ kho câu hỏi
          </h1>
          <p className={cx('subtitle')}>
            Chọn loại kỳ thi, cấu hình từng part (random theo số lượng hoặc chọn thủ công), rồi tạo đề. Câu hỏi lấy theo kho cá nhân (tài khoản đang đăng nhập).
          </p>
        </header>

        {notification.message && (
          <Alert variant={notification.type} className="mb-3" dismissible onClose={() => setNotification({})}>
            {notification.message}
          </Alert>
        )}

        <Form onSubmit={handleSubmit}>
          <div className={cx('configCard')}>
            <div className={cx('sectionTitle')}>
              <IoSettingsOutline /> 1. Thông tin đề thi
            </div>
            <Row className="g-3">
              <Col md={8}>
                <div className={cx('formGroup')}>
                  <label>Tiêu đề đề thi *</label>
                  <input
                    type="text"
                    className={cx('input')}
                    value={testInfo.title}
                    onChange={(e) => setTestInfo({ ...testInfo, title: e.target.value })}
                    placeholder="Nhập tiêu đề đề thi"
                    aria-label="Tiêu đề đề thi"
                  />
                </div>
              </Col>
              <Col md={4}>
                <div className={cx('formGroup')}>
                  <label><IoTimeOutline /> Thời gian (phút)</label>
                  <input
                    type="number"
                    min={0}
                    className={cx('input')}
                    value={testInfo.durationMinutes}
                    onChange={(e) => setTestInfo({ ...testInfo, durationMinutes: e.target.value })}
                    placeholder="VD: 60"
                    aria-label="Thời gian làm bài"
                  />
                </div>
              </Col>
              <Col md={6}>
                <div className={cx('formGroup')}>
                  <label>Loại kỳ thi *</label>
                  <select
                    className={cx('input')}
                    value={testInfo.examTypeId}
                    onChange={(e) => handleExamTypeChange(e.target.value)}
                    aria-label="Loại kỳ thi"
                  >
                    <option value="">-- Chọn --</option>
                    {(examTypes || []).map((t) => (
                      <option key={t.examTypeId} value={t.examTypeId}>{t.name}</option>
                    ))}
                  </select>
                </div>
              </Col>
              <Col md={6}>
                <div className={cx('formGroup')}>
                  <label><IoRocketOutline /> Số lượt làm tối đa</label>
                  <input
                    type="number"
                    min={0}
                    className={cx('input')}
                    value={testInfo.maxAttempts}
                    onChange={(e) => setTestInfo({ ...testInfo, maxAttempts: e.target.value })}
                    placeholder="Để trống = không giới hạn"
                    aria-label="Số lượt làm"
                  />
                </div>
              </Col>
              <Col md={12}>
                <div className={cx('formGroup')}>
                  <label>Mô tả</label>
                  <textarea
                    className={cx('input')}
                    rows={2}
                    value={testInfo.description}
                    onChange={(e) => setTestInfo({ ...testInfo, description: e.target.value })}
                    placeholder="Mô tả ngắn (tùy chọn)"
                    aria-label="Mô tả đề thi"
                  />
                </div>
              </Col>
            </Row>
          </div>

          {testInfo.examTypeId && (examParts || []).length > 0 && (
            <div className={cx('configCard')}>
              <div className={cx('sectionTitle')}>
                <IoLibraryOutline /> 2. Cấu hình từng Part
              </div>
              <p className={cx('hint')}>
                Mỗi part: <strong>Random theo số lượng</strong> (BE lấy ngẫu nhiên từ kho cá nhân) hoặc <strong>Chọn thủ công</strong>. Với part có passage (vd. Part 3, 4, 6, 7): chọn theo <strong>nhóm (cùng passage)</strong> để giữ tính tương đồng, không chọn lẻ từng câu.
              </p>

              {(examParts || []).map((part) => {
                const cfg = partConfigs[part.examPartId] ?? defaultPartConfig();
                const count = getPartEffectiveCount(part.examPartId);
                const maxInBank = (cfg.bankQuestions || []).length;
                const allSelected = maxInBank > 0 && (cfg.selectedIds || []).length === maxInBank;

                return (
                  <div key={part.examPartId} className={cx('partCard')}>
                    <button
                      type="button"
                      className={cx('partCardHeader')}
                      onClick={() => togglePartExpanded(part.examPartId)}
                      aria-expanded={cfg.expanded}
                    >
                      <span className={cx('partName')}>
                        <IoBookOutline size={20} /> {part.name}
                      </span>
                      <span className={cx('partBadge')}>{count} câu</span>
                      {cfg.expanded ? <IoChevronUpOutline size={22} /> : <IoChevronDownOutline size={22} />}
                    </button>

                    {cfg.expanded && (
                      <div className={cx('partCardBody')}>
                        <div className={cx('modeTabs')}>
                          <button
                            type="button"
                            className={cx('modeTab', { active: cfg.mode === SELECTION_MODES.RANDOM })}
                            onClick={() => updatePartConfig(part.examPartId, 'mode', SELECTION_MODES.RANDOM)}
                            aria-pressed={cfg.mode === SELECTION_MODES.RANDOM}
                          >
                            <IoShuffleOutline size={18} /> Random theo số lượng
                          </button>
                          <button
                            type="button"
                            className={cx('modeTab', { active: cfg.mode === SELECTION_MODES.MANUAL })}
                            onClick={() => updatePartConfig(part.examPartId, 'mode', SELECTION_MODES.MANUAL)}
                            aria-pressed={cfg.mode === SELECTION_MODES.MANUAL}
                          >
                            <IoCheckboxOutline size={18} /> Chọn thủ công
                          </button>
                        </div>

                        {cfg.mode === SELECTION_MODES.RANDOM && (
                          <div className={cx('randomRow')}>
                            <label className={cx('randomLabel')}>Số câu lấy ngẫu nhiên:</label>
                            <input
                              type="number"
                              min={0}
                              max={Math.max(maxInBank, 0)}
                              className={cx('input', 'randomInput')}
                              value={cfg.randomCount}
                              onChange={(e) => updatePartConfig(part.examPartId, 'randomCount', e.target.value)}
                              aria-label={`Số câu random ${part.name}`}
                            />
                            <span className={cx('randomHint')}>Tối đa {maxInBank} câu trong kho</span>
                          </div>
                        )}

                        {cfg.loading && (
                          <div className={cx('loadingWrap')}>
                            <Spinner animation="border" size="sm" /> <span>Đang tải câu hỏi...</span>
                          </div>
                        )}

                        {!cfg.loading && maxInBank === 0 && (
                          <Alert variant="info" className="mb-0 mt-2">Chưa có câu hỏi trong kho (cá nhân) cho part này.</Alert>
                        )}

                        {!cfg.loading && cfg.mode === SELECTION_MODES.MANUAL && maxInBank > 0 && (() => {
                          const groups = groupQuestionsByPassage(cfg.bankQuestions);
                          return (
                            <>
                              <div className={cx('selectAllRow')}>
                                <Form.Check
                                  type="checkbox"
                                  id={`select-all-${part.examPartId}`}
                                  label={`Chọn tất cả (${maxInBank} câu)`}
                                  checked={allSelected}
                                  onChange={(e) => toggleSelectAll(part.examPartId, e.target.checked)}
                                  aria-label={`Chọn tất cả ${part.name}`}
                                />
                              </div>
                              <div className={cx('groupList')}>
                                {groups.map((gr) => {
                                  const grSelected = isGroupSelected(part.examPartId, gr.groupKey);
                                  const grLabel = gr.passageId != null
                                    ? `Nhóm passage (${gr.questions.length} câu)`
                                    : `Câu độc lập (${gr.questions.length} câu)`;
                                  return (
                                    <div key={gr.groupKey} className={cx('passageGroup', { selected: grSelected })}>
                                      <div className={cx('groupHeader')}>
                                        <Form.Check
                                          type="checkbox"
                                          id={`gr-${part.examPartId}-${gr.groupKey}`}
                                          checked={grSelected}
                                          onChange={() => toggleGroup(part.examPartId, gr.groupKey)}
                                          aria-label={grLabel}
                                        />
                                        <span className={cx('groupLabel')}>{grLabel}</span>
                                      </div>
                                      <ul className={cx('questionList')} role="list">
                                        {gr.questions.map((q, index) => {
                                          const id = q.questionId ?? q.id;
                                          if (id == null) return null;
                                          const checked = (cfg.selectedIds || []).includes(id);
                                          return (
                                            <li key={id} className={cx('questionItem', { selected: checked })}>
                                              <span className={cx('questionIndex')}>{index + 1}.</span>
                                              <span className={cx('questionText')}>{q.questionText || '(Không có nội dung)'}</span>
                                            </li>
                                          );
                                        })}
                                      </ul>
                                    </div>
                                  );
                                })}
                              </div>
                            </>
                          );
                        })()}
                      </div>
                    )}
                  </div>
                );
              })}

              <p className={cx('totalCount')}>
                Tổng số câu sẽ đưa vào đề: <strong>{totalSelected}</strong>
              </p>
            </div>
          )}

          <div className={cx('footer')}>
            <Button
              type="submit"
              className={cx('btnSubmit')}
              disabled={loadingSubmit || !hasAnyPartWithQuestions || !testInfo.examTypeId}
            >
              {loadingSubmit ? <><Spinner animation="border" size="sm" /> Đang tạo đề...</> : <><IoRocketOutline /> Tạo đề thi từ kho</>}
            </Button>
          </div>
        </Form>
      </div>
    </div>
  );
};

export default CreateTestFromBankPage;

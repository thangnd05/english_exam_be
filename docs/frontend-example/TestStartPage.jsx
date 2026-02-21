/**
 * Trang làm bài thi – hiển thị câu theo nhóm passage (không tách/trộn).
 * Trong mỗi part: nhóm câu theo passage_id (consecutive), mỗi passage chỉ render 1 lần rồi liệt kê hết câu thuộc passage đó.
 *
 * Copy vào project, đổi import path (styles, api, TestStartDashboard) cho đúng.
 */
import axios from 'axios';
import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Container, Spinner, Button, Form, Row, Col } from 'react-bootstrap';
import classNames from 'classnames/bind';
import {
  IoSendOutline,
  IoLockClosedOutline,
  IoVolumeHighOutline,
  IoAlertCircleOutline,
  IoTimeOutline,
  IoCheckmarkCircleOutline,
} from 'react-icons/io5';
import { getPassageMediaByPassageId } from '~/api/passageMediaApi';
import TestStartDashboard from './TestStartDashboard';
import { IoListOutline, IoCloseOutline } from 'react-icons/io5';
import styles from './TestStartPage.module.scss';

const cx = classNames.bind(styles);

/** Lấy passageId từ câu hỏi (để nhóm). */
const getQuestionPassageKey = (q) => {
  const pid = q.passageId ?? q.passage?.passageId ?? q.passage?.passage_id ?? null;
  return pid != null ? `passage-${pid}` : `no-passage-${q.questionId ?? q.id}`;
};

/**
 * Nhóm câu theo passage liên tiếp: cùng passage_id và đứng cạnh nhau = 1 nhóm.
 * Giữ đúng thứ tự câu, mỗi passage chỉ hiển thị 1 lần rồi list hết câu thuộc passage đó.
 */
const groupQuestionsByPassageInOrder = (questions) => {
  if (!questions?.length) return [];
  const groups = [];
  let currentKey = null;
  let currentGroup = null;

  questions.forEach((q) => {
    const key = getQuestionPassageKey(q);
    if (key !== currentKey) {
      currentKey = key;
      currentGroup = {
        passage: q.passage ?? null,
        passageKey: key,
        questions: [q],
      };
      groups.push(currentGroup);
    } else {
      currentGroup.questions.push(q);
    }
  });

  return groups;
};

function TestStartPage() {
  const { testId } = useParams();
  const navigate = useNavigate();

  const [userTestId, setUserTestId] = useState(null);
  const [test, setTest] = useState({ parts: [] });
  const [userAnswers, setUserAnswers] = useState({});
  const [timeLeft, setTimeLeft] = useState(null);
  const [preCountdown, setPreCountdown] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [status, setStatus] = useState('loading');
  const [showInfoPanel, setShowInfoPanel] = useState(false);

  const getFullMediaUrl = (url) => {
    if (!url) return null;
    const cleanUrl = url.trim();
    if (cleanUrl.startsWith('http')) return cleanUrl;
    const backendUrl =
      process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080';
    return `${backendUrl.endsWith('/') ? backendUrl.slice(0, -1) : backendUrl}/${cleanUrl.startsWith('/') ? cleanUrl.slice(1) : cleanUrl}`;
  };

  const hasMediaList = (p) => {
    const list = p?.passageMedias ?? p?.passageMediaList ?? p?.passage_media;
    return Array.isArray(list) && list.length > 0;
  };

  const enrichTestWithPassageMedia = async (testData) => {
    const parts = testData.parts || [];
    const passageIdsToFetch = new Set();
    parts.forEach((part) => {
      const p = part.passage;
      const pid = p?.passageId ?? p?.passage_id;
      if (pid && !hasMediaList(p)) passageIdsToFetch.add(pid);
      (part.questions || []).forEach((q) => {
        const qp = q.passage;
        const qpid =
          qp?.passageId ?? qp?.passage_id ?? q.passageId ?? q.passage_id;
        if (qpid && !hasMediaList(qp)) passageIdsToFetch.add(qpid);
      });
    });
    if (passageIdsToFetch.size === 0) return testData;

    const ids = [...passageIdsToFetch];
    const results = await Promise.all(
      ids.map((id) => getPassageMediaByPassageId(id).catch(() => [])),
    );
    const mediaByPassageId = {};
    ids.forEach((id, i) => {
      mediaByPassageId[id] = Array.isArray(results[i]) ? results[i] : [];
    });

    const enrichedParts = parts.map((part) => {
      const partCopy = { ...part };
      const ppid = partCopy.passage?.passageId ?? partCopy.passage?.passage_id;
      if (ppid && mediaByPassageId[ppid]) {
        partCopy.passage = {
          ...partCopy.passage,
          passageMedias: mediaByPassageId[ppid],
        };
      }
      partCopy.questions = (part.questions || []).map((q) => {
        const qCopy = { ...q };
        const qpid =
          qCopy.passage?.passageId ??
          qCopy.passage?.passage_id ??
          qCopy.passageId ??
          qCopy.passage_id;
        if (qpid && mediaByPassageId[qpid]) {
          const passage = qCopy.passage
            ? { ...qCopy.passage, passageMedias: mediaByPassageId[qpid] }
            : { passageId: qpid, passageMedias: mediaByPassageId[qpid] };
          qCopy.passage = passage;
        }
        return qCopy;
      });
      return partCopy;
    });
    return { ...testData, parts: enrichedParts };
  };

  useEffect(() => {
    if (!testId) return;
    const savedState = sessionStorage.getItem(`userTestState-${testId}`);
    let restored = false;

    if (savedState) {
      const parsed = JSON.parse(savedState);
      setUserTestId(parsed.userTestId || null);
      setUserAnswers(parsed.userAnswers || {});
      if (parsed.timeLeft && parsed.lastSavedAt) {
        const elapsed = Math.floor((Date.now() - parsed.lastSavedAt) / 1000);
        setTimeLeft(Math.max(0, parsed.timeLeft - elapsed));
      } else {
        setTimeLeft(parsed.timeLeft ?? null);
      }
      restored = true;
    }

    axios
      .get(`/api/tests/usertest/${testId}`)
      .then(async (res) => {
        const testData = { ...res.data, parts: res.data.parts || [] };
        const enriched = await enrichTestWithPassageMedia(testData);
        setTest(enriched);

        if (testData.canDoTest === false) {
          setStatus('no-attempts');
          return;
        }

        const now = new Date();
        const availableFrom = testData.availableFrom
          ? new Date(testData.availableFrom)
          : null;
        const availableTo = testData.availableTo
          ? new Date(testData.availableTo)
          : null;

        if (availableFrom && now < availableFrom) {
          setStatus('locked');
          setPreCountdown(Math.floor((availableFrom - now) / 1000));
          return;
        }

        if (availableTo && now > availableTo) {
          setStatus('closed');
          return;
        }

        if (!restored) {
          const durationSeconds = (testData.durationMinutes || 0) * 60;
          let finalTime = durationSeconds;
          if (availableTo) {
            const diffSeconds = Math.floor((availableTo - now) / 1000);
            if (diffSeconds > 0)
              finalTime = Math.min(durationSeconds, diffSeconds);
            else finalTime = 0;
          }
          setTimeLeft(finalTime);
          setStatus('open');
        } else {
          setStatus('active');
        }
      })
      .catch(() => setStatus('error'));
  }, [testId, navigate]);

  useEffect(() => {
    if (status === 'open' && test?.testId) {
      const existing = sessionStorage.getItem(`userTest-${test.testId}`);
      if (existing) {
        setUserTestId(existing);
        setStatus('active');
        return;
      }
      axios
        .post('/api/user-tests', { testId: test.testId })
        .then((res) => {
          const id = res.data.userTestId;
          setUserTestId(id);
          sessionStorage.setItem(`userTest-${test.testId}`, id);
          setStatus('active');
        })
        .catch((err) => {
          if (err.response?.status === 403) setStatus('no-attempts');
          else setStatus('error');
        });
    }
  }, [status, test]);

  useEffect(() => {
    if (status === 'active' && userTestId) {
      sessionStorage.setItem(
        `userTestState-${testId}`,
        JSON.stringify({
          userTestId,
          userAnswers,
          timeLeft,
          lastSavedAt: Date.now(),
        }),
      );
    }
  }, [userAnswers, timeLeft, userTestId, status, testId]);

  useEffect(() => {
    if (status === 'locked' && preCountdown !== null) {
      if (preCountdown <= 0) {
        setStatus('open');
        return;
      }
      const timer = setInterval(() => setPreCountdown((p) => p - 1), 1000);
      return () => clearInterval(timer);
    }
  }, [preCountdown, status]);

  useEffect(() => {
    if (status === 'active' && timeLeft !== null) {
      const timer = setInterval(() => {
        setTimeLeft((prev) => {
          if (prev <= 1) {
            clearInterval(timer);
            handleSubmit();
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
      return () => clearInterval(timer);
    }
  }, [status, timeLeft]);

  const handleAnswerChange = (questionId, type, value) => {
    const updatedAnswer =
      type === 'MCQ' ? { selectedAnswerId: value } : { answerText: value };
    setUserAnswers({ ...userAnswers, [questionId]: updatedAnswer });
  };

  const handleSubmit = async () => {
    if (!userTestId || isSubmitting) return;
    setIsSubmitting(true);
    try {
      const payload = Object.entries(userAnswers).map(([qid, ans]) => ({
        userTestId,
        questionId: parseInt(qid, 10),
        selectedAnswerId: ans.selectedAnswerId || null,
        answerText: ans.answerText || null,
      }));
      if (payload.length > 0)
        await axios.post('/api/user-answers/batch', payload);
      const res = await axios.post(`/api/user-tests/${userTestId}/submit`);
      sessionStorage.removeItem(`userTest-${testId}`);
      sessionStorage.removeItem(`userTestState-${testId}`);
      navigate(`/tests/result/${userTestId}`, {
        state: { score: res.data.totalScore },
      });
    } catch (err) {
      alert('Nộp bài thất bại! Vui lòng thử lại.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const scrollToQuestion = (questionId) => {
    const element = document.getElementById(`q-${questionId}`);
    if (element) {
      const offset = 80;
      const elementPosition = element.getBoundingClientRect().top;
      const offsetPosition = elementPosition + window.pageYOffset - offset;
      window.scrollTo({ top: offsetPosition, behavior: 'smooth' });
    }
  };

  const allQuestions =
    test.parts?.reduce((acc, part) => {
      return [...acc, ...(part.questions || [])];
    }, []) || [];

  const formatTime = (seconds) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  const hasPassageImage = (passage, fallbackObj) => {
    const mediaList =
      passage?.passageMediaList ??
      passage?.passageMedias ??
      passage?.mediaList ??
      passage?.passage_media ??
      [];
    if (
      Array.isArray(mediaList) &&
      mediaList.some(
        (m) => (m.mediaType ?? m.media_type ?? '').toUpperCase() === 'IMAGE',
      )
    )
      return true;
    const singleUrl =
      passage?.mediaUrl ??
      passage?.media_url ??
      fallbackObj?.mediaUrl ??
      fallbackObj?.media_url;
    const pType =
      passage?.passageType ?? passage?.passage_type ?? fallbackObj?.passageType;
    return !!(singleUrl && (pType === 'READING' || pType === 'reading'));
  };

  const renderQuestionCard = (q, absoluteIndex) => (
    <div className={cx('question-card')}>
      <span className={cx('q-text')}>
        <span className={cx('q-number')}>Câu {absoluteIndex}:</span>{' '}
        {q.questionText}
      </span>

      {q.questionType === 'MCQ' && (
        <div className={cx('mcq-group')}>
          {q.answers?.map((a) => (
            <div
              key={a.answerId}
              className={cx('mcq-option', {
                selected:
                  userAnswers[q.questionId]?.selectedAnswerId === a.answerId,
              })}
              onClick={() =>
                handleAnswerChange(q.questionId, 'MCQ', a.answerId)
              }
            >
              <Form.Check
                type="radio"
                name={`q-${q.questionId}`}
                checked={
                  userAnswers[q.questionId]?.selectedAnswerId === a.answerId
                }
                readOnly
              />
              <span>
                {a.answerLabel}. {a.answerText}
              </span>
            </div>
          ))}
        </div>
      )}

      {q.questionType === 'FILL_BLANK' && (
        <input
          type="text"
          className={cx('fill-input')}
          value={userAnswers[q.questionId]?.answerText || ''}
          onChange={(e) =>
            handleAnswerChange(q.questionId, 'FILL_BLANK', e.target.value)
          }
          placeholder="Nhập câu trả lời của bạn..."
        />
      )}

      {q.questionType === 'ESSAY' && (
        <textarea
          className={cx('essay-input')}
          value={userAnswers[q.questionId]?.answerText || ''}
          onChange={(e) =>
            handleAnswerChange(q.questionId, 'ESSAY', e.target.value)
          }
          placeholder="Viết câu trả lời chi tiết tại đây..."
        />
      )}
    </div>
  );

  const renderPassage = (passage, fallbackObj) => {
    const content =
      passage?.content ??
      passage?.passage_content ??
      fallbackObj?.content ??
      fallbackObj?.passage_content;
    const pType = passage?.passageType ?? passage?.passage_type ?? 'READING';

    const mediaList =
      passage?.passageMediaList ??
      passage?.passageMedias ??
      passage?.mediaList ??
      passage?.passage_media ??
      [];
    const hasMediaList = Array.isArray(mediaList) && mediaList.length > 0;

    const singleMediaUrl =
      passage?.mediaUrl ??
      passage?.media_url ??
      fallbackObj?.mediaUrl ??
      fallbackObj?.media_url ??
      fallbackObj?.audioUrl ??
      fallbackObj?.audio_url ??
      fallbackObj?.passageMediaUrl;

    const hasContent = !!content;
    const hasAnyMedia = hasMediaList || !!singleMediaUrl;
    if (!hasContent && !hasAnyMedia) return null;

    return (
      <div className={cx('passage-box')}>
        {hasMediaList &&
          mediaList.map((m, idx) => {
            const url = m.mediaUrl ?? m.media_url;
            if (!url) return null;
            const type = (m.mediaType ?? m.media_type ?? '').toUpperCase();
            if (type === 'AUDIO') {
              return (
                <div key={idx} className="mb-3">
                  <div className="d-flex align-items-center gap-2 mb-2 text-primary fw-bold">
                    <IoVolumeHighOutline size={24} />
                    <span>
                      NGHE{' '}
                      {mediaList.filter(
                        (x) =>
                          (x.mediaType ?? x.media_type ?? '').toUpperCase() ===
                          'AUDIO',
                      ).length > 1
                        ? `(${idx + 1})`
                        : 'ĐOẠN HỘI THOẠI'}
                    </span>
                  </div>
                  <audio
                    controls
                    src={getFullMediaUrl(url)}
                    className={cx('audio-player')}
                  />
                </div>
              );
            }
            if (type === 'IMAGE') {
              return (
                <div key={idx} className="mb-3">
                  <img
                    src={getFullMediaUrl(url)}
                    alt={`Passage ${idx + 1}`}
                    className={cx('passage-image')}
                  />
                </div>
              );
            }
            return null;
          })}
        {!hasMediaList &&
          singleMediaUrl &&
          (pType === 'LISTENING' || pType === 'listening') && (
            <div className="mb-4">
              <div className="d-flex align-items-center gap-2 mb-3 text-primary fw-bold">
                <IoVolumeHighOutline size={24} />
                <span>NGHE ĐOẠN HỘI THOẠI</span>
              </div>
              <audio
                controls
                src={getFullMediaUrl(singleMediaUrl)}
                className={cx('audio-player')}
              />
            </div>
          )}
        {content && <div className={cx('passage-content')}>{content}</div>}
      </div>
    );
  };

  if (status === 'loading')
    return (
      <div className={cx('state-box')}>
        <Spinner animation="grow" variant="primary" />
        <h3>Đang niêm phong đề thi...</h3>
      </div>
    );

  if (status === 'no-attempts')
    return (
      <div className={cx('state-box')}>
        <IoAlertCircleOutline size={80} color="#ef4444" />
        <h3>Hết lượt làm bài</h3>
        <p>Bạn đã hoàn thành số lượt làm bài cho phép cho bài thi này.</p>
        <Button
          variant="primary"
          className="mt-4 rounded-pill"
          onClick={() => navigate(-1)}
        >
          Quay lại
        </Button>
      </div>
    );

  if (status === 'locked')
    return (
      <div className={cx('state-box')}>
        <IoLockClosedOutline size={80} color="#64748b" />
        <h3>Phòng thi chưa mở</h3>
        <p>Vui lòng đợi trong giây lát...</p>
        <div className={cx('timer-box', 'mt-4')}>
          <span className={cx('time')}>{formatTime(preCountdown)}</span>
        </div>
      </div>
    );

  if (status === 'closed')
    return (
      <div className={cx('state-box')}>
        <IoAlertCircleOutline size={80} color="#ef4444" />
        <h3>Phòng thi đã đóng</h3>
        <p>Rất tiếc, thời gian tham gia bài thi này đã kết thúc.</p>
        <Button
          variant="secondary"
          className="mt-4 rounded-pill"
          onClick={() => navigate(-1)}
        >
          Quay lại
        </Button>
      </div>
    );

  return (
    <div className={cx('wrapper')}>
      <Container fluid className={cx('content')}>
        <h1>Bài thi</h1>

        <Row>
          <Col xs={12}>
            {test.parts?.map((part, partIndex) => {
              const questionGroups = groupQuestionsByPassageInOrder(
                part.questions || [],
              );

              return (
                <div key={part.testPartId} className={cx('part-section')}>
                  <h3>
                    Phần {partIndex + 1}: {part.partName || 'Luyện tập'}
                  </h3>

                  {questionGroups.map((group, groupIndex) => {
                    const hasPassage = !!group.passage;

                    return (
                      <div
                        key={`${part.testPartId}-${group.passageKey}-${groupIndex}`}
                        className={cx('passage-group-block')}
                      >
                        {hasPassage ? (
                          <div
                            className={cx('question-card-wrapper', 'split-layout')}
                          >
                            <div
                              className={cx('passage-column')}
                              aria-label="Đọc tài liệu"
                            >
                              {renderPassage(
                                group.passage,
                                group.questions[0],
                              )}
                            </div>
                            <div className={cx('question-column')}>
                              <div className={cx('questions-list')}>
                                {group.questions.map((q) => {
                                  const absoluteIndex =
                                    allQuestions.findIndex(
                                      (allQ) =>
                                        allQ.questionId === q.questionId,
                                    ) + 1;
                                  return (
                                    <div
                                      key={q.questionId}
                                      id={`q-${q.questionId}`}
                                    >
                                      {renderQuestionCard(q, absoluteIndex)}
                                    </div>
                                  );
                                })}
                              </div>
                            </div>
                          </div>
                        ) : (
                          <div className={cx('questions-list')}>
                            {group.questions.map((q) => {
                              const absoluteIndex =
                                allQuestions.findIndex(
                                  (allQ) =>
                                    allQ.questionId === q.questionId,
                                ) + 1;
                              return (
                                <div
                                  key={q.questionId}
                                  id={`q-${q.questionId}`}
                                  className={cx('question-card-wrapper')}
                                >
                                  {renderQuestionCard(q, absoluteIndex)}
                                </div>
                              );
                            })}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              );
            })}
          </Col>
        </Row>
      </Container>

      <div className={cx('footer-actions')}>
        {showInfoPanel && (
          <div className={cx('footer-panel')}>
            <TestStartDashboard
              timeLeft={timeLeft}
              formatTime={formatTime}
              allQuestions={allQuestions}
              userAnswers={userAnswers}
              onScrollToQuestion={(id) => {
                scrollToQuestion(id);
                setShowInfoPanel(false);
              }}
            />
          </div>
        )}
        <div className={cx('footer-buttons')}>
          <Container className={cx('footer-buttons-inner')}>
            <button
              type="button"
              className={cx('btn-toggle-info', {
                active: showInfoPanel,
              })}
              onClick={() => setShowInfoPanel((v) => !v)}
              aria-expanded={showInfoPanel}
              aria-label={
                showInfoPanel
                  ? 'Ẩn thời gian và danh sách câu'
                  : 'Xem thời gian và danh sách câu'
              }
            >
              {showInfoPanel ? (
                <IoCloseOutline size={22} />
              ) : (
                <IoListOutline size={22} />
              )}
              <span>{showInfoPanel ? 'Ẩn' : 'Thời gian & Câu hỏi'}</span>
            </button>
            <div className={cx('footer-right-group')}>
              <div className={cx('footer-pills')}>
                <div className={cx('exam-stat', 'exam-stat-time')}>
                  <IoTimeOutline aria-hidden />
                  <span className={cx('exam-stat-value')}>
                    {formatTime(timeLeft)}
                  </span>
                </div>
                <div className={cx('exam-stat', 'exam-stat-done')}>
                  <IoCheckmarkCircleOutline aria-hidden />
                  <span className={cx('exam-stat-value')}>
                    {Object.keys(userAnswers).length}/{allQuestions.length}
                  </span>
                </div>
              </div>
              <button
                className={cx('btn-submit')}
                onClick={handleSubmit}
                disabled={isSubmitting}
              >
                {isSubmitting ? (
                  <Spinner animation="border" size="sm" />
                ) : (
                  <IoSendOutline />
                )}
                {isSubmitting ? 'Đang nộp bài...' : 'Nộp bài thi'}
              </button>
            </div>
          </Container>
        </div>
      </div>
    </div>
  );
}

export default TestStartPage;

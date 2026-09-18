import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { Modal } from "../../shared/components/Modal.jsx";
import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { attributeLabel, pluralize, variationValueLabel } from "../../shared/utils/format.js";
import { segmentApi } from "./segment.api.js";

export function SegmentMembersDrawer({ environmentKey, onClose, segmentKey }) {
    const [data, setData] = useState(null);
    const [status, setStatus] = useState({ loading: true, error: "" });

    useEffect(() => {
        let active = true;
        (async () => {
            try {
                const result = await segmentApi.members(segmentKey, environmentKey);
                if (active) { setData(result); setStatus({ loading: false, error: "" }); }
            } catch (error) {
                if (active) setStatus({ loading: false, error: error.message });
            }
        })();
        return () => { active = false; };
    }, [segmentKey, environmentKey]);

    return createPortal(<Modal className="members-modal" label={`Members of ${segmentKey}`} onClose={onClose}>
        <section className="members-drawer">
            <header className="modal-header">
                <h2>{data?.segment?.name || segmentKey}</h2>
                <button aria-label="Close" className="icon-button" type="button" onClick={onClose}>
                    <MaterialIcon size={20}>close</MaterialIcon>
                </button>
            </header>
            <div className="members-body">
                {status.loading && <p className="panel-empty" role="status">Matching the audience…</p>}
                {status.error && <p className="form-error" role="alert">{status.error}</p>}
                {data && <>
                    <p className="members-count">
                        {pluralize(data.members.length, "member")} of {data.audienceSize} match this segment right now.
                    </p>
                    {data.members.length === 0
                        ? <div className="empty-state compact">
                            <h2>Nobody matches yet</h2>
                            <p>Adjust the rules or include members directly.</p>
                        </div>
                        : <ul className="members-list">
                            {data.members.map((member) => <li key={member.key}>
                                <div className="members-identity">
                                    <strong>{member.name}</strong>
                                    <small>{member.email}</small>
                                    <code>{member.key}</code>
                                </div>
                                <ul className="members-attributes">
                                    {Object.entries(member.attributes).map(([attribute, value]) => <li key={attribute}>
                                        <span>{attributeLabel(attribute)}</span>
                                        <strong>{variationValueLabel(value)}</strong>
                                    </li>)}
                                </ul>
                                {member.included && <span className="members-included">Always included</span>}
                            </li>)}
                        </ul>}
                </>}
            </div>
        </section>
    </Modal>, document.body);
}
